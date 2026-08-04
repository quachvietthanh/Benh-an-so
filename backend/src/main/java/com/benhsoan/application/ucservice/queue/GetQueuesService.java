package com.benhsoan.application.ucservice.queue;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.queue.GetQueuesQuery;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.GetQueuesUseCase;
import com.benhsoan.port.outbound.repository.queryRepository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetQueuesService implements GetQueuesUseCase {

    private final QueueItemQueryRepository queueItemQueryRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public List<QueueItemResult> getQueues(GetQueuesQuery query) {
        if (query.queueDate() == null) {
            throw new ValidationException("Queue date is required.");
        }
        if (!canViewBoard()) {
            throw new UnauthorizedQueueOperationException();
        }
        return queueItemQueryRepository.findQueueBoard(query.queueDate(), query.doctorId(), query.roomId());
    }

    private boolean canViewBoard() {
        return currentUserPort.hasRole("ADMIN") || currentUserPort.hasRole("DOCTOR")
                || currentUserPort.hasRole("RECEPTIONIST") || currentUserPort.hasRole("NURSE");
    }
}
