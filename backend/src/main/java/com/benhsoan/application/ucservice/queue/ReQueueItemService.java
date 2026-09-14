package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.domain.queue.exception.QueueNotFoundException;
import com.benhsoan.port.dto.command.queue.ReQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.ReQueueItemUseCase;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReQueueItemService implements ReQueueItemUseCase {

    private static final ZoneId CLINIC_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private final QueueItemRepository queueItemRepository;
    private final MedicalQueueRepository medicalQueueRepository;
    private final QueueOperationAuthorization authorization;
    private final QueueItemQueryRepository queueItemQueryRepository;
    private final ClockPort clockPort;
    private final QueueAuditService queueAuditService;

    @Override
    public QueueItemResult reQueue(ReQueueItemCommand command) {
        QueueItem item = queueItemRepository.findByIdForUpdate(command.queueItemId())
                .orElseThrow(() -> new QueueItemNotFoundException(command.queueItemId()));
        var queue = medicalQueueRepository.findById(item.getMedicalQueueId())
                .orElseThrow(() -> new QueueNotFoundException(item.getMedicalQueueId()));
        authorization.requireReQueuePermission(queue);

        if (queue.getStatus() != MedicalQueueStatus.OPEN) {
            throw new CheckInConflictException("Medical queue is closed.");
        }

        var now = clockPort.now();
        LocalDate today = now.atZone(CLINIC_ZONE_ID).toLocalDate();
        if (!queue.getQueueDate().isEqual(today)) {
            throw new CheckInConflictException("Cannot re-queue into a queue from a different date.");
        }

        item.reQueue(now);
        queueItemRepository.save(item);
        queueAuditService.recordReQueued(item);

        return queueItemQueryRepository.findDetailById(item.getId())
                .orElseThrow(() -> new QueueItemNotFoundException(item.getId()));
    }
}
