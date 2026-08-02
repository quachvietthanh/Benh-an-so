package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.queue.CallNextQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.CallNextQueueItemUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.queryRepository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CallNextQueueItemService implements CallNextQueueItemUseCase {

    private final MedicalQueueRepository medicalQueueRepository;
    private final QueueItemRepository queueItemRepository;
    private final VisitRepository visitRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueOperationAuthorization authorization;
    private final QueueItemQueryRepository queueItemQueryRepository;
    private final ClockPort clockPort;
    private final QueueAuditService queueAuditService;

    @Override
    public QueueItemResult callNext(CallNextQueueItemCommand command) {
        MedicalQueue queue = medicalQueueRepository.findByIdForUpdate(command.medicalQueueId())
                .orElseThrow(() -> new CheckInConflictException("Medical queue does not exist."));
        authorization.requireCallPermission(queue);
        if (queue.getStatus() != MedicalQueueStatus.OPEN) {
            throw new CheckInConflictException("Medical queue is closed.");
        }
        QueueItem item = queueItemRepository.findNextWaitingForUpdate(queue.getId())
                .orElseThrow(() -> new CheckInConflictException("No waiting queue item is available."));
        var visit = visitRepository.findByIdForUpdate(item.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(item.getVisitId()));
        var now = clockPort.now();
        item.call(now);
        visit.start(now);
        if (item.getAppointmentId() != null) {
            var appointment = appointmentRepository.findByIdForUpdate(item.getAppointmentId())
                    .orElseThrow(() -> new AppointmentNotFoundException(item.getAppointmentId()));
            appointment.start();
            appointmentRepository.save(appointment);
        }
        queueItemRepository.save(item);
        visitRepository.save(visit);
        queueAuditService.record(com.benhsoan.domain.auditlog.enums.ActionType.UPDATE, item);
        return queueItemQueryRepository.findDetailById(item.getId())
                .orElseThrow(() -> new QueueItemNotFoundException(item.getId()));
    }
}
