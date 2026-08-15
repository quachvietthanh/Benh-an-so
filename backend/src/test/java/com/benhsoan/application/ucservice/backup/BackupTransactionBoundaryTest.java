package com.benhsoan.application.ucservice.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;

class BackupTransactionBoundaryTest {

    @Test
    void marksFailedRecordThroughLifecycleTransaction() {
        BackupRecordRepository repository = mock(BackupRecordRepository.class);
        BackupRecordLifecycleService lifecycleService = new BackupRecordLifecycleService(repository);
        BackupRecord record = BackupRecord.create(
                "BKP-20260814-0001", BackupType.FULL, null, UUID.randomUUID(), Instant.now());
        when(repository.findById(record.getId())).thenReturn(Optional.of(record));
        when(repository.save(any(BackupRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        lifecycleService.markFailed(record.getId());

        assertEquals(BackupStatus.FAILED, record.getStatus());
        verify(repository).save(record);
    }

    @Test
    void lifecycleTransitionsUseNewTransactionsAndExportUsesConsistentReadTransaction() throws Exception {
        assertRequiresNew(BackupRecordLifecycleService.class.getMethod(
                "createInProgress", String.class, BackupType.class, String.class, UUID.class, Instant.class));
        assertRequiresNew(BackupRecordLifecycleService.class.getMethod(
                "markSuccess", UUID.class, BackupSnapshot.class));
        assertRequiresNew(BackupRecordLifecycleService.class.getMethod("markFailed", UUID.class));

        Transactional exportTransaction = BackupSnapshotExportService.class
                .getMethod("export", String.class)
                .getAnnotation(Transactional.class);
        assertNotNull(exportTransaction);
        assertEquals(Propagation.REQUIRES_NEW, exportTransaction.propagation());
        assertEquals(Isolation.REPEATABLE_READ, exportTransaction.isolation());
        assertTrue(exportTransaction.readOnly());
    }

    private void assertRequiresNew(Method method) {
        Transactional transaction = method.getAnnotation(Transactional.class);
        assertNotNull(transaction);
        assertEquals(Propagation.REQUIRES_NEW, transaction.propagation());
    }
}
