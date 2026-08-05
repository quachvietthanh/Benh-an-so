package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.queue.UpdateQueueItemStatusCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.UpdateQueueItemStatusUseCase;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateQueueItemStatusService implements UpdateQueueItemStatusUseCase {

    private final QueueItemRepository queueItemRepository;
    private final MedicalQueueRepository medicalQueueRepository;
    private final VisitRepository visitRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueOperationAuthorization authorization;
    private final QueueItemQueryRepository queueItemQueryRepository;
    private final ClockPort clockPort;
    private final QueueAuditService queueAuditService;

    @Override
    public QueueItemResult updateStatus(UpdateQueueItemStatusCommand command) {
        QueueItem item = queueItemRepository.findByIdForUpdate(command.queueItemId())
                .orElseThrow(() -> new QueueItemNotFoundException(command.queueItemId()));
        var queue = medicalQueueRepository.findById(item.getMedicalQueueId())
                .orElseThrow(() -> new QueueItemNotFoundException(item.getMedicalQueueId()));
        authorization.requireClinicalUpdatePermission(queue);
        var visit = visitRepository.findByIdForUpdate(item.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(item.getVisitId()));
        var now = clockPort.now();

        if (command.targetStatus() == QueueItemStatus.WAITING_FOR_RESULT) {
            item.waitForResult(now);
            visit.waitForResult(now);
        } else if (command.targetStatus() == QueueItemStatus.IN_PROGRESS) {
            item.resume(now);
            visit.resume(now);
        } else if (command.targetStatus() == QueueItemStatus.CANCELLED) {
            item.cancel(command.cancelReason(), now);
            visit.cancel(now);
            if (item.getAppointmentId() != null) {
                var appointment = appointmentRepository.findByIdForUpdate(item.getAppointmentId())
                        .orElseThrow(() -> new AppointmentNotFoundException(item.getAppointmentId()));
                appointment.cancel(command.cancelReason());
                appointmentRepository.save(appointment);
            }
        } else {
            throw new ValidationException("Only WAITING_FOR_RESULT, IN_PROGRESS, or CANCELLED are supported updates.");
        }

        queueItemRepository.save(item);
        visitRepository.save(visit);
        queueAuditService.record(item.getStatus() == QueueItemStatus.CANCELLED
                ? com.benhsoan.domain.auditlog.enums.ActionType.CANCEL
                : com.benhsoan.domain.auditlog.enums.ActionType.UPDATE, item);
        return queueItemQueryRepository.findDetailById(item.getId())
                .orElseThrow(() -> new QueueItemNotFoundException(item.getId()));
    }
}
