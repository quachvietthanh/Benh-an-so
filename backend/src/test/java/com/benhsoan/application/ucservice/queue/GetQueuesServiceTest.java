package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.port.dto.command.queue.GetQueuesQuery;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class GetQueuesServiceTest {

    @Mock private QueueItemQueryRepository queueItemQueryRepository;
    @Mock private CurrentUserPort currentUserPort;

    @InjectMocks private GetQueuesService service;

    @Test
    void allowsManagerToViewQueueBoard() {
        GetQueuesQuery query = new GetQueuesQuery(LocalDate.of(2026, 8, 14), null, null);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.hasRole("NURSE")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(queueItemQueryRepository.findQueueBoard(query.queueDate(), query.doctorId(), query.roomId()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.getQueues(query));
        verify(queueItemQueryRepository).findQueueBoard(query.queueDate(), query.doctorId(), query.roomId());
    }

    @Test
    void rejectsUnauthorizedRoleFromViewingQueueBoard() {
        GetQueuesQuery query = new GetQueuesQuery(LocalDate.of(2026, 8, 14), null, null);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.hasRole("NURSE")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);

        assertThrows(UnauthorizedQueueOperationException.class, () -> service.getQueues(query));
    }
}
