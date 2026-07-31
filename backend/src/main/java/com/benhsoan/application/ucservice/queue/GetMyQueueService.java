package com.benhsoan.application.ucservice.queue;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.queue.GetMyQueueQuery;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.GetMyQueueUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyQueueService implements GetMyQueueUseCase {

    private final QueueItemRepository queueItemRepository;
    private final QueueItemResultMapper resultMapper;
    private final CurrentUserPort currentUserPort;

    @Override
    public List<QueueItemResult> getMyQueue(GetMyQueueQuery query) {
        if (query.queueDate() == null) {
            throw new ValidationException("Queue date is required.");
        }
        if (!currentUserPort.hasRole("DOCTOR")) {
            throw new UnauthorizedQueueOperationException();
        }
        return queueItemRepository.findQueueBoard(query.queueDate(), currentUserPort.getCurrentUserId(), null).stream()
                .map(resultMapper::toResult)
                .toList();
    }
}
