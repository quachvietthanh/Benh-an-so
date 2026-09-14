package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.domain.queue.exception.QueueItemInvalidStatusException;
import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.port.dto.command.queue.ReQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class ReQueueItemServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");

    @Test
    void reQueuesSkippedItemSuccessfully() {
        TestContext ctx = context(true);

        QueueItemResult response = ctx.service.reQueue(new ReQueueItemCommand(ctx.item.getId()));

        assertEquals(QueueItemStatus.WAITING, ctx.item.getStatus());
        verify(ctx.queueItemRepository).save(ctx.item);
        verify(ctx.auditService).recordReQueued(ctx.item);
        assertEquals(QueueItemStatus.WAITING, response.status());
    }

    @Test
    void rejectsReQueueWhenItemIsNotSkipped() {
        TestContext ctx = context(true);
        // Change status to WAITING
        ctx.item.reQueue(NOW.plusSeconds(90));

        assertThrows(QueueItemInvalidStatusException.class,
                () -> ctx.service.reQueue(new ReQueueItemCommand(ctx.item.getId())));
    }

    @Test
    void rejectsReQueueWhenQueueIsClosed() {
        TestContext ctx = context(true);
        ctx.queue.close(NOW.plusSeconds(300));

        assertThrows(CheckInConflictException.class,
                () -> ctx.service.reQueue(new ReQueueItemCommand(ctx.item.getId())));
    }

    @Test
    void rejectsReQueueWhenUserNotAuthorized() {
        TestContext ctx = context(false);

        assertThrows(UnauthorizedQueueOperationException.class,
                () -> ctx.service.reQueue(new ReQueueItemCommand(ctx.item.getId())));
    }

    private TestContext context(boolean authorized) {
        UUID doctorId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 8, 2), NOW);
        QueueItem item = QueueItem.create(queue.getId(), UUID.randomUUID(), null, UUID.randomUUID(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), NOW);
        item.call(NOW.plusSeconds(30));
        item.skip("Absent", NOW.plusSeconds(60));

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        QueueAuditService auditService = mock(QueueAuditService.class);
        QueueItemQueryRepository queryRepository = mock(QueueItemQueryRepository.class);

        when(currentUserPort.hasRole("ADMIN")).thenReturn(authorized);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(authorized);
        when(clockPort.now()).thenReturn(NOW.plusSeconds(90));

        when(queueItemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(medicalQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));

        QueueItemResult result = new QueueItemResult(item.getId(), queue.getId(), item.getPatientId(),
                "Nguyen Van A", doctorId, "Bac si B", queue.getRoomId(), "P101", null,
                item.getVisitId(), "VIS000100", item.getSourceType(), QueueItemStatus.WAITING,
                item.getQueueNumber(), item.getQueueDate(), item.getCheckedInAt(), item.getCalledAt(), null, null, null,
                item.getSkippedAt(), item.getSkipReason(), item.getCallCount());
        when(queryRepository.findDetailById(item.getId())).thenReturn(Optional.of(result));

        ReQueueItemService service = new ReQueueItemService(
                queueItemRepository, medicalQueueRepository,
                new QueueOperationAuthorization(currentUserPort),
                queryRepository, clockPort, auditService
        );

        return new TestContext(service, queue, item, queueItemRepository, auditService);
    }

    private record TestContext(
            ReQueueItemService service,
            MedicalQueue queue,
            QueueItem item,
            QueueItemRepository queueItemRepository,
            QueueAuditService auditService
    ) {}
}
