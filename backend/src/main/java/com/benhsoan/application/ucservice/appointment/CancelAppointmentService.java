package com.benhsoan.application.ucservice.appointment;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.command.appointment.CancelAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.CancelAppointmentUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.logRepository.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

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

    @Override
    public AppointmentResult cancel(
            UUID appointmentId,
            CancelAppointmentCommand command
    ) {

        validatePermission();

        Appointment appointment =
                appointmentRepository.findById(appointmentId)
                        .orElseThrow(() ->
                                new AppointmentNotFoundException(
                                        appointmentId
                                )
                        );

        appointment.cancel(
                command.cancelReason()
        );

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
