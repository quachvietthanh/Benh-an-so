package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueuePriority;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.domain.queue.exception.QueueItemInvalidStatusException;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.port.dto.command.queue.PrioritizeQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class PrioritizeQueueItemServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");

    @Test
    void prioritizesWaitingItemSuccessfully() {
        TestContext ctx = context(true);

        QueueItemResult response = ctx.service.prioritize(
                new PrioritizeQueueItemCommand(ctx.item.getId(), QueuePriority.EMERGENCY, "Ca cap cuu - Sot cao co giat"));

        assertEquals(QueuePriority.EMERGENCY, ctx.item.getPriority());
        assertEquals("Ca cap cuu - Sot cao co giat", ctx.item.getPriorityReason());
        assertEquals(ctx.actorId, ctx.item.getPrioritizedBy());
        assertEquals(NOW, ctx.item.getPrioritizedAt());
        verify(ctx.queueItemRepository).save(ctx.item);
        verify(ctx.auditService).recordPrioritized(ctx.item, QueuePriority.EMERGENCY, "Ca cap cuu - Sot cao co giat");
        assertEquals(QueuePriority.EMERGENCY, response.priority());
    }

    @Test
    void rejectsPrioritizeWhenQueueIsClosed() {
        TestContext ctx = context(true);
        ctx.queue.close(NOW.plusSeconds(300));

        assertThrows(CheckInConflictException.class,
                () -> ctx.service.prioritize(new PrioritizeQueueItemCommand(ctx.item.getId(), QueuePriority.EMERGENCY, "Emergency")));
    }

    @Test
    void rejectsPrioritizeWhenUserLacksPermission() {
        TestContext ctx = context(false);

        assertThrows(UnauthorizedQueueOperationException.class,
                () -> ctx.service.prioritize(new PrioritizeQueueItemCommand(ctx.item.getId(), QueuePriority.EMERGENCY, "Emergency")));
    }

    @Test
    void rejectsPrioritizeWhenItemNotFound() {
        TestContext ctx = context(true);
        UUID nonExistentId = UUID.randomUUID();
        when(ctx.queueItemRepository.findByIdForUpdate(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(QueueItemNotFoundException.class,
                () -> ctx.service.prioritize(new PrioritizeQueueItemCommand(nonExistentId, QueuePriority.EMERGENCY, "Emergency")));
    }

    @Test
    void rejectsPrioritizeWhenItemIsNotWaiting() {
        TestContext ctx = context(true);
        ctx.item.call(NOW.plusSeconds(10)); // status is now IN_PROGRESS

        assertThrows(QueueItemInvalidStatusException.class,
                () -> ctx.service.prioritize(new PrioritizeQueueItemCommand(ctx.item.getId(), QueuePriority.EMERGENCY, "Emergency")));
    }

    @Test
    void rejectsPrioritizeWhenReasonIsBlank() {
        TestContext ctx = context(true);

        assertThrows(IllegalArgumentException.class,
                () -> ctx.service.prioritize(new PrioritizeQueueItemCommand(ctx.item.getId(), QueuePriority.EMERGENCY, "   ")));
    }

    @Test
    void rejectsPrioritizeWhenQueueDateIsNotToday() {
        TestContext ctx = context(true);
        MedicalQueue yesterdayQueue = MedicalQueue.create(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 8, 1), NOW);
        QueueItem yesterdayItem = QueueItem.create(yesterdayQueue.getId(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 1), ctx.actorId, NOW);

        when(ctx.queueItemRepository.findByIdForUpdate(yesterdayItem.getId())).thenReturn(Optional.of(yesterdayItem));
        when(ctx.medicalQueueRepository.findById(yesterdayQueue.getId())).thenReturn(Optional.of(yesterdayQueue));

        var ex = assertThrows(CheckInConflictException.class,
                () -> ctx.service.prioritize(new PrioritizeQueueItemCommand(yesterdayItem.getId(), QueuePriority.EMERGENCY, "Emergency")));
        assertEquals("Cannot prioritize queue item from a different date.", ex.getMessage());
    }

    private record TestContext(
            PrioritizeQueueItemService service,
            QueueItemRepository queueItemRepository,
            MedicalQueueRepository medicalQueueRepository,
            QueueAuditService auditService,
            MedicalQueue queue,
            QueueItem item,
            UUID actorId
    ) {}

    private TestContext context(boolean authorized) {
        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        QueueItemQueryRepository queryRepository = mock(QueueItemQueryRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        QueueAuditService auditService = mock(QueueAuditService.class);

        UUID actorId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        LocalDate queueDate = LocalDate.of(2026, 8, 2);

        MedicalQueue queue = MedicalQueue.create(doctorId, roomId, queueDate, NOW);
        QueueItem item = QueueItem.create(queue.getId(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, queueDate, actorId, NOW);

        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(authorized);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        when(clockPort.now()).thenReturn(NOW);

        when(queueItemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(queueItemRepository.save(any(QueueItem.class))).thenAnswer(inv -> inv.getArgument(0));

        when(medicalQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));

        when(queryRepository.findDetailById(item.getId())).thenAnswer(inv -> Optional.of(
                new QueueItemResult(item.getId(), queue.getId(), item.getPatientId(), "BN001", "Patient A",
                        doctorId, "Doctor A", roomId, "R101", null, item.getVisitId(), "KB001",
                        item.getSourceType(), item.getStatus(), item.getQueueNumber(), item.getQueueDate(),
                        item.getCheckedInAt(), item.getCalledAt(), item.getCompletedAt(), item.getCancelledAt(),
                        item.getCancelReason(), item.getSkippedAt(), item.getSkipReason(), item.getCallCount(),
                        item.getPriority(), item.getPriorityReason(), item.getPrioritizedAt(), item.getPrioritizedBy())
        ));

        QueueOperationAuthorization authorization = new QueueOperationAuthorization(currentUserPort);
        PrioritizeQueueItemService service = new PrioritizeQueueItemService(
                queueItemRepository,
                medicalQueueRepository,
                authorization,
                queryRepository,
                currentUserPort,
                clockPort,
                auditService
        );

        return new TestContext(service, queueItemRepository, medicalQueueRepository, auditService, queue, item, actorId);
    }
}

