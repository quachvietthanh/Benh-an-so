package com.benhsoan.application.ucservice.appointment;

import java.util.UUID;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.appointment.CancelAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.CancelAppointmentUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelAppointmentService
        implements CancelAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;

    private final CurrentUserPort currentUserPort;

    private final AppointmentResultMapper appointmentResultMapper;

    private final AuditLogRepository auditLogRepository;

    private final QueueItemRepository queueItemRepository;

    private final VisitRepository visitRepository;

    private final ClockPort clockPort;

    @Override
    public AppointmentResult cancel(
            UUID appointmentId,
            CancelAppointmentCommand command
    ) {

        validatePermission();

        Optional<QueueItem> linkedQueueItem = queueItemRepository.findByAppointmentId(appointmentId);
        QueueItem lockedQueueItem = null;
        Visit lockedVisit = null;
        if (linkedQueueItem.isPresent()) {
            lockedQueueItem = queueItemRepository.findByIdForUpdate(linkedQueueItem.orElseThrow().getId())
                    .orElseThrow(() -> new CheckInConflictException(
                            "Queue item disappeared during appointment cancellation."));
            UUID visitId = lockedQueueItem.getVisitId();
            lockedVisit = visitRepository.findByIdForUpdate(visitId)
                    .orElseThrow(() -> new VisitNotFoundException(visitId));
        }

        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        appointment.cancel(command.cancelReason());

        if (lockedQueueItem != null) {
            var cancelledAt = clockPort.now();
            lockedQueueItem.cancel(command.cancelReason(), cancelledAt);
            lockedVisit.cancel(cancelledAt);
            queueItemRepository.save(lockedQueueItem);
            visitRepository.save(lockedVisit);
        }

        Appointment saved = appointmentRepository.save(appointment);

        auditLogRepository.save(
                AuditLog.create(
                        currentUserPort.getCurrentUserId(),
                        ActionType.CANCEL,
                        ResourceType.APPOINTMENT,
                        saved.getId(),
                        """
                        {
                        "appointmentCode":"%s",
                        "cancelReason":"%s"
                        }
                        """.formatted(
                                saved.getAppointmentCode(),
                                command.cancelReason()
                        ),
                        null
                )
        );

        return appointmentResultMapper.toResult(saved);
    }

    private void validatePermission() {

        if (!currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("RECEPTIONIST")) {
            throw new UnauthorizedAppointmentOperationException();
        }

    }

}
