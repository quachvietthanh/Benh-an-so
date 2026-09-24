package com.benhsoan.application.ucservice.backup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.config.BackupScheduleProperties;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.BackupSchedule;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;
import com.benhsoan.port.outbound.repository.backup.BackupScheduleRepository;
import com.benhsoan.port.outbound.time.ClockPort;

class ExecuteScheduledBackupServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T03:00:00Z");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private final BackupScheduleRepository scheduleRepository = mock(BackupScheduleRepository.class);
    private final BackupRecordRepository backupRecordRepository = mock(BackupRecordRepository.class);
    private final BackupRecordLifecycleService lifecycleService = mock(BackupRecordLifecycleService.class);
    private final BackupSnapshotExportService snapshotExportService = mock(BackupSnapshotExportService.class);
    private final BackupCodeGenerator backupCodeGenerator = mock(BackupCodeGenerator.class);
    private final BackupAuditLogWriter auditLogWriter = mock(BackupAuditLogWriter.class);
    private final BackupFailureReason backupFailureReason = new BackupFailureReason();

    private final AtomicReference<Instant> clock = new AtomicReference<>(NOW);
    private final ClockPort clockPort = () -> clock.get();

    private final BackupScheduleProperties properties = new BackupScheduleProperties(UTC);

    private ExecuteScheduledBackupService service;

    @BeforeEach
    void setUp() {
        service = new ExecuteScheduledBackupService(
                scheduleRepository,
                backupRecordRepository,
                lifecycleService,
                snapshotExportService,
                backupCodeGenerator,
                auditLogWriter,
                backupFailureReason,
                properties,
                clockPort
        );
        when(backupCodeGenerator.generate()).thenReturn("BKP-20260924-0001");
    }

    private BackupSchedule schedule(boolean enabled, LocalTime time) {
        return BackupSchedule.create(enabled, time, NOW);
    }

    @Test
    void doesNothingWhenScheduleMissing() {
        when(scheduleRepository.find()).thenReturn(Optional.empty());

        service.execute();

        verifyNoInteractions(snapshotExportService);
    }

    @Test
    void doesNothingWhenScheduleDisabled() {
        when(scheduleRepository.find()).thenReturn(Optional.of(schedule(false, LocalTime.of(2, 0))));

        service.execute();

        verifyNoInteractions(snapshotExportService);
    }

    @Test
    void doesNothingBeforeConfiguredTime() {
        clock.set(Instant.parse("2026-09-24T01:00:00Z"));
        when(scheduleRepository.find()).thenReturn(Optional.of(schedule(true, LocalTime.of(2, 0))));

        service.execute();

        verifyNoInteractions(snapshotExportService);
    }

    @Test
    void runsBackupAtConfiguredTime() {
        when(scheduleRepository.find()).thenReturn(Optional.of(schedule(true, LocalTime.of(2, 0))));
        when(backupRecordRepository.findTopByBackupTypeOrderByCreatedAtDesc(BackupType.SCHEDULED))
                .thenReturn(Optional.empty());
        BackupRecord created = BackupRecord.create(
                "BKP-20260924-0001", BackupType.SCHEDULED, null, BackupSystemActor.SYSTEM_USER_ID, NOW);
        when(lifecycleService.createInProgress(any(), any(), any(), any(), any())).thenReturn(created);
        when(snapshotExportService.export("BKP-20260924-0001"))
                .thenReturn(new BackupSnapshot("BKP-20260924-0001.json", new byte[]{1}));
        BackupRecord succeeded = BackupRecord.create(
                "BKP-20260924-0001", BackupType.SCHEDULED, null, BackupSystemActor.SYSTEM_USER_ID, NOW);
        succeeded.markSuccess("BKP-20260924-0001.json", 1L);
        when(lifecycleService.markSuccess(eq(created.getId()), any())).thenReturn(succeeded);

        service.execute();

        verify(lifecycleService).createInProgress(
                eq("BKP-20260924-0001"), eq(BackupType.SCHEDULED), any(),
                eq(BackupSystemActor.SYSTEM_USER_ID), any());
        verify(snapshotExportService).export("BKP-20260924-0001");
        verify(lifecycleService).markSuccess(eq(created.getId()), any());
        verify(auditLogWriter).write(
                eq(BackupSystemActor.SYSTEM_USER_ID), eq(ActionType.BACKUP), any(), anyString());
    }

    @Test
    void recordsFailureReasonAndDoesNotThrowWhenExportFails() {
        when(scheduleRepository.find()).thenReturn(Optional.of(schedule(true, LocalTime.of(2, 0))));
        when(backupRecordRepository.findTopByBackupTypeOrderByCreatedAtDesc(BackupType.SCHEDULED))
                .thenReturn(Optional.empty());
        BackupRecord created = BackupRecord.create(
                "BKP-20260924-0001", BackupType.SCHEDULED, null, BackupSystemActor.SYSTEM_USER_ID, NOW);
        when(lifecycleService.createInProgress(any(), any(), any(), any(), any())).thenReturn(created);
        when(snapshotExportService.export(any())).thenThrow(new RuntimeException("disk full"));
        BackupRecord failed = BackupRecord.create(
                "BKP-20260924-0001", BackupType.SCHEDULED, null, BackupSystemActor.SYSTEM_USER_ID, NOW);
        failed.markFailed("disk full");
        when(lifecycleService.markFailed(eq(created.getId()), eq("disk full"))).thenReturn(failed);

        assertDoesNotThrow(service::execute);

        verify(lifecycleService).markFailed(eq(created.getId()), eq("disk full"));
        verify(auditLogWriter).write(
                eq(BackupSystemActor.SYSTEM_USER_ID), eq(ActionType.BACKUP), any(), anyString());
    }

    @Test
    void doesNotRunAgainAfterSuccessfulRunToday() {
        when(scheduleRepository.find()).thenReturn(Optional.of(schedule(true, LocalTime.of(2, 0))));
        BackupRecord todayRun = BackupRecord.create(
                "BKP-20260924-0001", BackupType.SCHEDULED, null, BackupSystemActor.SYSTEM_USER_ID, NOW);
        todayRun.markSuccess("BKP-20260924-0001.json", 1L);
        when(backupRecordRepository.findTopByBackupTypeOrderByCreatedAtDesc(BackupType.SCHEDULED))
                .thenReturn(Optional.of(todayRun));

        service.execute();

        verifyNoInteractions(snapshotExportService);
    }

    @Test
    void doesNotRetryAfterFailedRunToday() {
        when(scheduleRepository.find()).thenReturn(Optional.of(schedule(true, LocalTime.of(2, 0))));
        BackupRecord todayFailed = BackupRecord.create(
                "BKP-20260924-0001", BackupType.SCHEDULED, null, BackupSystemActor.SYSTEM_USER_ID, NOW);
        todayFailed.markFailed("disk full");
        when(backupRecordRepository.findTopByBackupTypeOrderByCreatedAtDesc(BackupType.SCHEDULED))
                .thenReturn(Optional.of(todayFailed));

        service.execute();

        verifyNoInteractions(snapshotExportService);
    }

    @Test
    void concurrentPollRunsOnlyOnce() throws Exception {
        when(scheduleRepository.find()).thenReturn(Optional.of(schedule(true, LocalTime.of(2, 0))));
        when(backupRecordRepository.findTopByBackupTypeOrderByCreatedAtDesc(BackupType.SCHEDULED))
                .thenReturn(Optional.empty());
        BackupRecord created = BackupRecord.create(
                "BKP-20260924-0001", BackupType.SCHEDULED, null, BackupSystemActor.SYSTEM_USER_ID, NOW);
        when(lifecycleService.createInProgress(any(), any(), any(), any(), any())).thenReturn(created);
        when(lifecycleService.markSuccess(eq(created.getId()), any())).thenAnswer(i -> {
            BackupRecord r = BackupRecord.create(
                    "BKP-20260924-0001", BackupType.SCHEDULED, null, BackupSystemActor.SYSTEM_USER_ID, NOW);
            r.markSuccess("BKP-20260924-0001.json", 1L);
            return r;
        });

        CountDownLatch enteredExport = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger exportCalls = new AtomicInteger();
        when(snapshotExportService.export(any())).thenAnswer(i -> {
            exportCalls.incrementAndGet();
            enteredExport.countDown();
            release.await(5, TimeUnit.SECONDS);
            return new BackupSnapshot("BKP-20260924-0001.json", new byte[]{1});
        });

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> first = pool.submit(service::execute);
            assertTrue(enteredExport.await(5, TimeUnit.SECONDS));

            // Second poll while the first is still running must be ignored.
            service.execute();
            release.countDown();
            first.get(5, TimeUnit.SECONDS);

            assertEquals(1, exportCalls.get());
        } finally {
            pool.shutdownNow();
        }
    }
}
