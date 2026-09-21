package com.benhsoan.application.ucservice.queue;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.domain.queue.exception.QueueNotFoundException;
import com.benhsoan.port.dto.command.queue.PrioritizeQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.PrioritizeQueueItemUseCase;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PrioritizeQueueItemService implements PrioritizeQueueItemUseCase {

    private static final ZoneId CLINIC_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private final QueueItemRepository queueItemRepository;
    private final MedicalQueueRepository medicalQueueRepository;
    private final QueueOperationAuthorization authorization;
    private final QueueItemQueryRepository queueItemQueryRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final QueueAuditService queueAuditService;

    @Override
    public QueueItemResult prioritize(PrioritizeQueueItemCommand command) {
        QueueItem item = queueItemRepository.findByIdForUpdate(command.queueItemId())
                .orElseThrow(() -> new QueueItemNotFoundException(command.queueItemId()));
        var queue = medicalQueueRepository.findById(item.getMedicalQueueId())
                .orElseThrow(() -> new QueueNotFoundException(item.getMedicalQueueId()));

        authorization.requirePrioritizePermission(queue);

        if (queue.getStatus() != MedicalQueueStatus.OPEN) {
            throw new CheckInConflictException("Medical queue is closed.");
        }

        var now = clockPort.now();
        LocalDate today = now.atZone(CLINIC_ZONE_ID).toLocalDate();
        if (!queue.getQueueDate().isEqual(today)) {
            throw new CheckInConflictException("Cannot prioritize queue item from a different date.");
        }

        var actorId = currentUserPort.getCurrentUserId();

        item.prioritize(command.priority(), command.reason(), actorId, now);
        queueItemRepository.save(item);
        queueAuditService.recordPrioritized(item, command.priority(), command.reason());

        return queueItemQueryRepository.findDetailById(item.getId())
                .orElseThrow(() -> new QueueItemNotFoundException(item.getId()));
    }
}
