package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.port.dto.result.QueueHistoryResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class GetQueueHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");

    @Test
    void returnsQueueHistoryWithParsedDetails() {
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 8, 2), NOW);
        QueueItem item = QueueItem.create(queue.getId(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), NOW);
        item.call(NOW.plusSeconds(30));
        item.skip("Absent", NOW.plusSeconds(60));

        AuditLog log = AuditLog.restore(
                UUID.randomUUID(),
                userId,
                ActionType.UPDATE,
                ResourceType.VISIT,
                item.getVisitId(),
                "{\"action\":\"DEFERRED\",\"status\":\"SKIPPED\",\"callCount\":1,\"reason\":\"Patient absent\"}",
                "127.0.0.1",
                NOW.plusSeconds(60)
        );

        User user = User.restore(
                userId,
                "receptionist1",
                "hashedpwd",
                "Le Tan A",
                "receptionist1@clinic.com",
                "0900000000",
                UUID.randomUUID(),
                true,
                NOW,
                NOW
        );

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(queueItemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(medicalQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(auditLogRepository.findByResourceTypeAndResourceId(ResourceType.VISIT, item.getVisitId()))
                .thenReturn(List.of(log));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        GetQueueHistoryService service = new GetQueueHistoryService(
                queueItemRepository, medicalQueueRepository,
                new QueueOperationAuthorization(currentUserPort),
                auditLogRepository, userRepository, new ObjectMapper()
        );

        List<QueueHistoryResult> history = service.getHistory(item.getId());

        assertEquals(1, history.size());
        QueueHistoryResult entry = history.get(0);
        assertEquals(item.getId(), entry.queueItemId());
        assertEquals("Le Tan A", entry.operatorName());
        assertEquals("DEFERRED", entry.action());
        assertEquals("SKIPPED", entry.status());
        assertEquals(1, entry.callCount());
        assertEquals("Patient absent", entry.reason());
        assertEquals(NOW.plusSeconds(60), entry.timestamp());
    }

    @Test
    void throwsWhenQueueItemNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

        when(queueItemRepository.findByIdForUpdate(nonExistentId)).thenReturn(Optional.empty());

        GetQueueHistoryService service = new GetQueueHistoryService(
                queueItemRepository, medicalQueueRepository,
                new QueueOperationAuthorization(currentUserPort),
                auditLogRepository, userRepository, new ObjectMapper()
        );

        assertThrows(QueueItemNotFoundException.class, () -> service.getHistory(nonExistentId));
    }
}
