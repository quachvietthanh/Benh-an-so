package com.benhsoan.application.ucservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.config.AnomalyDetectionProperties;
import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertSeverity;
import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;
import com.benhsoan.port.outbound.repository.security.SecurityAlertRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ScanAccessLogAnomaliesServiceTest {

    @Mock
    private MedicalRecordAccessLogRepository accessLogRepository;

    @Mock
    private SecurityAlertRepository securityAlertRepository;

    @Mock
    private ClockPort clockPort;

    private ScanAccessLogAnomaliesService service;

    @BeforeEach
    void setUp() {
        AnomalyDetectionProperties properties = new AnomalyDetectionProperties(
                20,
                LocalTime.of(7, 0),
                LocalTime.of(18, 0),
                ZoneId.of("Asia/Ho_Chi_Minh")
        );
        service = new ScanAccessLogAnomaliesService(
                accessLogRepository,
                securityAlertRepository,
                properties,
                clockPort
        );
    }

    @Test
    void generatesHighSeverityAlertWhenViewsExceedHourlyThreshold() {
        UUID user = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T10:05:00Z"); // 17:05 local, within working hours
        when(clockPort.now()).thenReturn(now);

        List<MedicalRecordAccessLog> views = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            views.add(view(user, now.plusSeconds(i)));
        }
        when(accessLogRepository.findViewsBetween(
                Instant.parse("2026-08-11T09:05:00Z"),
                Instant.parse("2026-08-11T10:05:00Z")
        )).thenReturn(views);

        service.scan();

        ArgumentCaptor<SecurityAlert> captor = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(securityAlertRepository, times(1)).save(captor.capture());

        SecurityAlert alert = captor.getValue();
        assertEquals(AlertSeverity.HIGH, alert.getSeverity());
        assertEquals(AlertType.THRESHOLD_EXCEEDED, alert.getAlertType());
        assertEquals(21, alert.getAccessCount());
        assertEquals(user, alert.getUserId());
    }

    @Test
    void generatesLowSeverityAlertForOffHoursAccess() {
        UUID user = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T15:05:00Z"); // 22:05 local
        when(clockPort.now()).thenReturn(now);

        MedicalRecordAccessLog offHoursView = view(user, Instant.parse("2026-08-11T15:00:00Z")); // 22:00 local
        when(accessLogRepository.findViewsBetween(
                Instant.parse("2026-08-11T14:05:00Z"),
                Instant.parse("2026-08-11T15:05:00Z")
        )).thenReturn(List.of(offHoursView));

        service.scan();

        ArgumentCaptor<SecurityAlert> captor = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(securityAlertRepository, times(1)).save(captor.capture());

        SecurityAlert alert = captor.getValue();
        assertEquals(AlertSeverity.LOW, alert.getSeverity());
        assertEquals(AlertType.OFF_HOURS_ACCESS, alert.getAlertType());
        assertEquals(1, alert.getAccessCount());
        assertEquals(user, alert.getUserId());
    }

    @Test
    void doesNotCreateAlertWhenActivityIsNormal() {
        UUID user = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T10:05:00Z"); // 17:05 local, within working hours
        when(clockPort.now()).thenReturn(now);

        List<MedicalRecordAccessLog> views = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            views.add(view(user, now.plusSeconds(i)));
        }
        when(accessLogRepository.findViewsBetween(any(), any())).thenReturn(views);

        service.scan();

        verify(securityAlertRepository, never()).save(any());
        verify(securityAlertRepository, never())
                .findLatestActiveAlert(any(), any(), any());
    }

    @Test
    void doesNothingWhenNoViewsInWindow() {
        when(clockPort.now()).thenReturn(Instant.parse("2026-08-11T10:05:00Z"));
        when(accessLogRepository.findViewsBetween(any(), any())).thenReturn(List.of());

        service.scan();

        verifyNoInteractions(securityAlertRepository);
    }

    @Test
    void secondScanUpdatesExistingAlertInsteadOfCreatingDuplicate() {
        UUID user = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-08-11T10:05:00Z"); // 17:05 local, within working hours
        Instant t2 = t1.plus(5, ChronoUnit.MINUTES);

        when(clockPort.now()).thenReturn(t1, t2);
        when(accessLogRepository.findViewsBetween(any(), any()))
                .thenReturn(views(user, 21, t1))
                .thenReturn(views(user, 21, t2));

        AtomicReference<SecurityAlert> persisted = new AtomicReference<>();
        when(securityAlertRepository.save(any(SecurityAlert.class)))
                .thenAnswer(invocation -> {
                    SecurityAlert alert = invocation.getArgument(0);
                    persisted.set(alert);
                    return alert;
                });
        when(securityAlertRepository.findLatestActiveAlert(any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(persisted.get()));

        // First cycle: no recent alert of the same type -> insert a new alert.
        service.scan();
        SecurityAlert first = persisted.get();
        assertEquals(AlertSeverity.HIGH, first.getSeverity());
        assertEquals(21, first.getAccessCount());

        // Second cycle (t2 = t1 + 5 min): cooldown query returns the existing alert -> update it.
        service.scan();

        verify(securityAlertRepository, times(2)).save(any(SecurityAlert.class));
        SecurityAlert updated = persisted.get();
        assertEquals(first.getId(), updated.getId());
        assertEquals(t2, updated.getWindowEnd());
        assertEquals(21, updated.getAccessCount());
        assertTrue(updated.getDescription().contains("21"));
    }

    private List<MedicalRecordAccessLog> views(UUID userId, int count, Instant around) {
        List<MedicalRecordAccessLog> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(view(userId, around.minusSeconds(i)));
        }
        return result;
    }

    private MedicalRecordAccessLog view(UUID userId, Instant accessedAt) {
        return MedicalRecordAccessLog.createRecordAccess(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                MedicalRecordAccessAction.VIEW,
                "Medical record viewed",
                accessedAt
        );
    }
}
