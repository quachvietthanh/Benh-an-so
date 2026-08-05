package com.benhsoan.application.ucservice.appointment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.port.dto.command.appointment.MarkAppointmentNoShowCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.MarkAppointmentNoShowUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkAppointmentNoShowService
        implements MarkAppointmentNoShowUseCase {

    private final AppointmentRepository appointmentRepository;

    private final CurrentUserPort currentUserPort;

    private final AuditLogRepository auditLogRepository;

    private final AppointmentResultMapper appointmentResultMapper;

    private final QueueItemRepository queueItemRepository;

    @Override
    public AppointmentResult execute(
            MarkAppointmentNoShowCommand command
    ) {

        validate();

        Appointment appointment
                = appointmentRepository.findByIdForUpdate(command.appointmentId())
                        .orElseThrow(()
                                -> new AppointmentNotFoundException(
                                command.appointmentId()
                        ));

        if (queueItemRepository.findByAppointmentId(appointment.getId()).isPresent()) {
            throw new CheckInConflictException("Checked-in appointments cannot be marked as no show.");
        }

        appointment.markNoShow(command.markedAt());

        Appointment saved
                = appointmentRepository.save(appointment);

        auditLogRepository.save(
                AuditLog.create(
                        currentUserPort.getCurrentUserId(),
                        ActionType.UPDATE,
                        ResourceType.APPOINTMENT,
                        saved.getId(),
                        """
                        {
                        "appointmentCode":"%s",
                        "status":"%s"
                        }
                        """.formatted(
                                saved.getAppointmentCode(),
                                saved.getStatus()
                        ),
                        null
                )
        );

        return appointmentResultMapper.toResult(saved);
    }

    private void validate() {

        if (!currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("RECEPTIONIST")) {
            throw new UnauthorizedAppointmentOperationException();
        }

    }

}
