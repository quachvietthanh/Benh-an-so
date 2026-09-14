package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.domain.queue.exception.QueueNotFoundException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.queue.SkipQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.SkipQueueItemUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SkipQueueItemService implements SkipQueueItemUseCase {

    static final String APPOINTMENT_CANCEL_REASON = "PATIENT_ABSENT_AFTER_CHECK_IN";

    private final QueueItemRepository queueItemRepository;
    private final MedicalQueueRepository medicalQueueRepository;
    private final VisitRepository visitRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueOperationAuthorization authorization;
    private final QueueItemQueryRepository queueItemQueryRepository;
    private final ClockPort clockPort;
    private final QueueAuditService queueAuditService;

    @Override
    public QueueItemResult skip(SkipQueueItemCommand command) {
        QueueItem item = queueItemRepository.findByIdForUpdate(command.queueItemId())
                .orElseThrow(() -> new QueueItemNotFoundException(command.queueItemId()));
        var queue = medicalQueueRepository.findByIdForUpdate(item.getMedicalQueueId())
                .orElseThrow(() -> new QueueNotFoundException(item.getMedicalQueueId()));
        authorization.requireSkipPermission(queue);

        Visit visit = visitRepository.findByIdForUpdate(item.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(item.getVisitId()));
        Appointment appointment = loadAppointment(item);
        var skippedAt = clockPort.now();

        item.skip(command.reason(), skippedAt);
        visit.revertToWaiting(skippedAt);
        if (appointment != null) {
            appointment.revertToCheckedIn();
        }

        queueItemRepository.save(item);
        visitRepository.save(visit);
        if (appointment != null) {
            appointmentRepository.save(appointment);
        }

        if (queue.getStatus() == MedicalQueueStatus.OPEN) {
            var nextItemOpt = queueItemRepository.findNextWaitingForUpdate(queue.getId());
            if (nextItemOpt.isPresent()) {
                QueueItem nextItem = nextItemOpt.get();
                Visit nextVisit = visitRepository.findByIdForUpdate(nextItem.getVisitId())
                        .orElseThrow(() -> new VisitNotFoundException(nextItem.getVisitId()));
                nextItem.call(skippedAt);
                nextVisit.start(skippedAt);
                if (nextItem.getAppointmentId() != null) {
                    var nextAppt = appointmentRepository.findByIdForUpdate(nextItem.getAppointmentId())
                            .orElseThrow(() -> new AppointmentNotFoundException(nextItem.getAppointmentId()));
                    nextAppt.start();
                    appointmentRepository.save(nextAppt);
                }
                queueItemRepository.save(nextItem);
                visitRepository.save(nextVisit);
                queueAuditService.record(com.benhsoan.domain.auditlog.enums.ActionType.UPDATE, nextItem);
            }
        }

        queueAuditService.recordSkipped(item, command.reason());
        return queueItemQueryRepository.findDetailById(item.getId())
                .orElseThrow(() -> new QueueItemNotFoundException(item.getId()));
    }

    private Appointment loadAppointment(QueueItem item) {
        if (item.getAppointmentId() == null) {
            return null;
        }
        return appointmentRepository.findByIdForUpdate(item.getAppointmentId())
                .orElseThrow(() -> new AppointmentNotFoundException(item.getAppointmentId()));
    }
}
