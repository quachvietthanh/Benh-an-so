package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.port.outbound.repository.logRepository.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class QueueAuditServiceTest {

    @Test
    void recordsSkippedQueueItemWithStandardReason() {
        Instant now = Instant.parse("2026-08-02T02:00:00Z");
        QueueItem item = QueueItem.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), now);
        item.call(now.plusSeconds(30));
        item.skip("Patient absent when called", now.plusSeconds(60));
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new QueueAuditService(auditLogRepository, currentUserPort)
                .recordSkipped(item, SkipQueueItemService.APPOINTMENT_CANCEL_REASON);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(ActionType.UPDATE, captor.getValue().getActionType());
        assertTrue(captor.getValue().getDetail().contains("\"status\":\"SKIPPED\""));
        assertTrue(captor.getValue().getDetail()
                .contains("\"reason\":\"PATIENT_ABSENT_AFTER_CHECK_IN\""));
    }
}
