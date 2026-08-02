package com.benhsoan.application.ucservice.queue;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.GetQueueItemUseCase;
import com.benhsoan.port.outbound.repository.queryRepository.queue.QueueItemQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetQueueItemService implements GetQueueItemUseCase {

    private final QueueItemQueryRepository queueItemQueryRepository;
    private final QueueOperationAuthorization authorization;

    @Override
    public QueueItemResult getById(UUID queueItemId) {
        QueueItemResult item = queueItemQueryRepository.findDetailById(queueItemId)
                .orElseThrow(() -> new QueueItemNotFoundException(queueItemId));
        authorization.requireReadPermission(item.doctorId());
        return item;
    }
}
