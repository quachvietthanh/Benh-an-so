package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.benhsoan.domain.queue.exception.QueueNotFoundException;
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
    void returnsQueueHistoryWithParsedDetailsWithoutWriteLock() {
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
                "{\"queueItemId\":\"%s\",\"action\":\"DEFERRED\",\"status\":\"SKIPPED\",\"callCount\":1,\"reason\":\"Patient absent\"}"
                        .formatted(item.getId()),
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
        when(queueItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(medicalQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(auditLogRepository.findByResourceTypeAndResourceId(ResourceType.VISIT, item.getVisitId()))
                .thenReturn(List.of(log));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

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

        verify(queueItemRepository).findById(item.getId());
        verify(queueItemRepository, never()).findByIdForUpdate(item.getId());
    }

    @Test
    void filtersOutLogsNotBelongingToQueueItemAndDoesNotOverwriteReason() {
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 8, 2), NOW);
        QueueItem item = QueueItem.create(queue.getId(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), NOW);
        item.call(NOW.plusSeconds(30));
        item.skip("Current skip reason", NOW.plusSeconds(60));

        AuditLog nonQueueLog = AuditLog.restore(
                UUID.randomUUID(),
                userId,
                ActionType.CREATE,
                ResourceType.VISIT,
                item.getVisitId(),
                "{\"prescriptionId\":\"some-id\"}",
                "127.0.0.1",
                NOW
        );

        AuditLog checkInLog = AuditLog.restore(
                UUID.randomUUID(),
                userId,
                ActionType.UPDATE,
                ResourceType.VISIT,
                item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"status\":\"WAITING\"}".formatted(item.getId()),
                "127.0.0.1",
                NOW.plusSeconds(10)
        );

        AuditLog otherItemLog = AuditLog.restore(
                UUID.randomUUID(),
                userId,
                ActionType.UPDATE,
                ResourceType.VISIT,
                item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"status\":\"SKIPPED\"}".formatted(UUID.randomUUID()),
                "127.0.0.1",
                NOW.plusSeconds(20)
        );

        User user = User.restore(
                userId, "user1", "hashed", "User One", "user1@clinic.com", "0900000001",
                UUID.randomUUID(), true, NOW, NOW
        );

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(queueItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(medicalQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(auditLogRepository.findByResourceTypeAndResourceId(ResourceType.VISIT, item.getVisitId()))
                .thenReturn(List.of(nonQueueLog, checkInLog, otherItemLog));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        GetQueueHistoryService service = new GetQueueHistoryService(
                queueItemRepository, medicalQueueRepository,
                new QueueOperationAuthorization(currentUserPort),
                auditLogRepository, userRepository, new ObjectMapper()
        );

        List<QueueHistoryResult> history = service.getHistory(item.getId());

        assertEquals(1, history.size());
        QueueHistoryResult entry = history.get(0);
        assertEquals(item.getId(), entry.queueItemId());
        assertEquals("WAITING", entry.status());
        assertEquals(0, entry.callCount());
        assertNull(entry.reason());
    }

    @Test
    void throwsWhenQueueItemNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

        when(queueItemRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        GetQueueHistoryService service = new GetQueueHistoryService(
                queueItemRepository, medicalQueueRepository,
                new QueueOperationAuthorization(currentUserPort),
                auditLogRepository, userRepository, new ObjectMapper()
        );

        assertThrows(QueueItemNotFoundException.class, () -> service.getHistory(nonExistentId));
    }

    @Test
    void throwsWhenMedicalQueueNotFound() {
        UUID queueItemId = UUID.randomUUID();
        UUID queueId = UUID.randomUUID();
        QueueItem item = QueueItem.create(queueId, UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), NOW);

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

        when(queueItemRepository.findById(queueItemId)).thenReturn(Optional.of(item));
        when(medicalQueueRepository.findById(queueId)).thenReturn(Optional.empty());

        GetQueueHistoryService service = new GetQueueHistoryService(
                queueItemRepository, medicalQueueRepository,
                new QueueOperationAuthorization(currentUserPort),
                auditLogRepository, userRepository, new ObjectMapper()
        );

        assertThrows(QueueNotFoundException.class, () -> service.getHistory(queueItemId));
    }

    @Test
    void checkInHistoryPreservesHistoricalWaitingStatusEvenWhenItemCompleted() {
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 8, 2), NOW);
        QueueItem item = QueueItem.create(queue.getId(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), NOW);
        item.call(NOW.plusSeconds(30));
        item.complete(NOW.plusSeconds(120));
        assertEquals(com.benhsoan.domain.queue.enums.QueueItemStatus.COMPLETED, item.getStatus());

        AuditLog checkInLog = AuditLog.restore(
                UUID.randomUUID(),
                userId,
                ActionType.CREATE,
                ResourceType.VISIT,
                item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"sourceType\":\"WALK_IN\",\"queueNumber\":1,\"status\":\"WAITING\",\"action\":\"CHECK_IN\",\"callCount\":0}"
                        .formatted(item.getId()),
                "127.0.0.1",
                NOW
        );

        User user = User.restore(
                userId, "receptionist1", "hashedpwd", "Le Tan A", "receptionist1@clinic.com", "0900000000",
                UUID.randomUUID(), true, NOW, NOW
        );

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(queueItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(medicalQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(auditLogRepository.findByResourceTypeAndResourceId(ResourceType.VISIT, item.getVisitId()))
                .thenReturn(List.of(checkInLog));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        GetQueueHistoryService service = new GetQueueHistoryService(
                queueItemRepository, medicalQueueRepository,
                new QueueOperationAuthorization(currentUserPort),
                auditLogRepository, userRepository, new ObjectMapper()
        );

        List<QueueHistoryResult> history = service.getHistory(item.getId());

        assertEquals(1, history.size());
        QueueHistoryResult entry = history.get(0);
        assertEquals("CHECK_IN", entry.action());
        assertEquals("WAITING", entry.status(), "Check-in historical status must remain WAITING even if QueueItem is COMPLETED");
        assertEquals(0, entry.callCount());
    }

    @Test
    void returnsSemanticActionsWithoutFallbackToGenericActionType() {
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 8, 2), NOW);
        QueueItem item = QueueItem.create(queue.getId(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), NOW);

        AuditLog checkInLog = AuditLog.restore(
                UUID.randomUUID(), userId, ActionType.CREATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"action\":\"CHECK_IN\",\"status\":\"WAITING\",\"callCount\":0}".formatted(item.getId()),
                "127.0.0.1", NOW
        );
        AuditLog callLog = AuditLog.restore(
                UUID.randomUUID(), userId, ActionType.UPDATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"action\":\"CALL\",\"status\":\"IN_PROGRESS\",\"callCount\":1}".formatted(item.getId()),
                "127.0.0.1", NOW.plusSeconds(30)
        );
        AuditLog deferLog = AuditLog.restore(
                UUID.randomUUID(), userId, ActionType.UPDATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"action\":\"DEFERRED\",\"status\":\"SKIPPED\",\"callCount\":1,\"reason\":\"Absent\"}".formatted(item.getId()),
                "127.0.0.1", NOW.plusSeconds(60)
        );
        AuditLog reQueueLog = AuditLog.restore(
                UUID.randomUUID(), userId, ActionType.UPDATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"action\":\"RE_QUEUED\",\"status\":\"WAITING\",\"callCount\":1}".formatted(item.getId()),
                "127.0.0.1", NOW.plusSeconds(90)
        );
        AuditLog completedLog = AuditLog.restore(
                UUID.randomUUID(), userId, ActionType.UPDATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\",\"action\":\"COMPLETED\",\"status\":\"COMPLETED\",\"callCount\":2}".formatted(item.getId()),
                "127.0.0.1", NOW.plusSeconds(120)
        );

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(queueItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(medicalQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(auditLogRepository.findByResourceTypeAndResourceId(ResourceType.VISIT, item.getVisitId()))
                .thenReturn(List.of(checkInLog, callLog, deferLog, reQueueLog, completedLog));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of());

        GetQueueHistoryService service = new GetQueueHistoryService(
                queueItemRepository, medicalQueueRepository,
                new QueueOperationAuthorization(currentUserPort),
                auditLogRepository, userRepository, new ObjectMapper()
        );

        List<QueueHistoryResult> history = service.getHistory(item.getId());

        assertEquals(5, history.size());
        assertEquals("CHECK_IN", history.get(0).action());
        assertEquals("CALL", history.get(1).action());
        assertEquals("DEFERRED", history.get(2).action());
        assertEquals("RE_QUEUED", history.get(3).action());
        assertEquals("COMPLETED", history.get(4).action());
    }

    @Test
    void handlesLegacyPayloadWithoutStatusOrActionGracefully() {
        UUID doctorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 8, 2), NOW);
        QueueItem item = QueueItem.create(queue.getId(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), NOW);
        item.call(NOW.plusSeconds(30));
        item.complete(NOW.plusSeconds(120));

        AuditLog legacyLog = AuditLog.restore(
                UUID.randomUUID(), userId, ActionType.UPDATE, ResourceType.VISIT, item.getVisitId(),
                "{\"queueItemId\":\"%s\"}".formatted(item.getId()),
                "127.0.0.1", NOW
        );

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(queueItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(medicalQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(auditLogRepository.findByResourceTypeAndResourceId(ResourceType.VISIT, item.getVisitId()))
                .thenReturn(List.of(legacyLog));

        GetQueueHistoryService service = new GetQueueHistoryService(
                queueItemRepository, medicalQueueRepository,
                new QueueOperationAuthorization(currentUserPort),
                auditLogRepository, userRepository, new ObjectMapper()
        );

        List<QueueHistoryResult> history = service.getHistory(item.getId());

        assertEquals(1, history.size());
        assertNull(history.get(0).action(), "Legacy log without action should return null action");
        assertNull(history.get(0).status(), "Legacy log without status should not fallback to COMPLETED");
    }
}

