package com.benhsoan.application.ucservice.queue;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.GetQueueItemUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.MedicalQueueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetQueueItemService implements GetQueueItemUseCase {

    private final QueueItemRepository queueItemRepository;
    private final MedicalQueueRepository medicalQueueRepository;
    private final QueueItemResultMapper resultMapper;
    private final QueueOperationAuthorization authorization;

    @Override
    public QueueItemResult getById(UUID queueItemId) {
        var item = queueItemRepository.findById(queueItemId)
                .orElseThrow(() -> new QueueItemNotFoundException(queueItemId));
        var queue = medicalQueueRepository.findById(item.getMedicalQueueId())
                .orElseThrow(() -> new QueueItemNotFoundException(item.getMedicalQueueId()));
        authorization.requireReadPermission(queue);
        return resultMapper.toResult(item);
    }
}
