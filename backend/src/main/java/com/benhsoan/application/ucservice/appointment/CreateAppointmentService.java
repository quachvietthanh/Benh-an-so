package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentTimeConflictException;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.InvalidAppointmentTimeRangeException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.appointment.CreateAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.CreateAppointmentUseCase;
import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.logRepository.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateAppointmentService
        implements CreateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;

    private final PatientRepository patientRepository;

    private final UserRepository userRepository;

    private final AppointmentCodeGenerator appointmentCodeGenerator;

    private final CurrentUserPort currentUserPort;

    private final AppointmentResultMapper appointmentResultMapper;

    private final AuditLogRepository auditLogRepository;

    @Override
    public AppointmentResult create(
            CreateAppointmentCommand command
    ) {

        validate(command);

        UUID currentUserId =
                currentUserPort.getCurrentUserId();

        String appointmentCode =
                appointmentCodeGenerator.generate();

        Appointment appointment =
                Appointment.create(
                        appointmentCode,
                        command.patientId(),
                        command.doctorId(),
                        command.startTime(),
                        command.endTime(),
                        command.reason(),
                        currentUserId
                );

        Appointment saved = appointmentRepository.save(appointment);

        auditLogRepository.save(
                AuditLog.create(
                        currentUserId,
                        ActionType.CREATE,
                        ResourceType.APPOINTMENT,
                        saved.getId(),
                        """
                        {
                        "appointmentCode":"%s",
                        "patientId":"%s",
                        "doctorId":"%s",
                        "startTime":"%s",
                        "endTime":"%s"
                        }
                        """.formatted(
                                saved.getAppointmentCode(),
                                saved.getPatientId(),
                                saved.getDoctorId(),
                                saved.getStartTime(),
                                saved.getEndTime()
                        ),
                        null
                )
        );

        return appointmentResultMapper.toResult(saved);
    }

    private void validate(
            CreateAppointmentCommand command
    ) {

        patientRepository.findById(command.patientId())
                .orElseThrow(() ->
                        new PatientNotFoundException(command.patientId()));

        User doctor =
                userRepository.findById(command.doctorId())
                        .orElseThrow(() ->
                                new DoctorNotFoundException(
                                        command.doctorId()));

        if (!doctor.isActive()) {
            throw new DoctorInactiveException(
                    doctor.getId()
            );
        }

        if (!currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("RECEPTIONIST")) {
            throw new UnauthorizedAppointmentOperationException();
        }

        if (!command.endTime().isAfter(command.startTime())) {
            throw new InvalidAppointmentTimeRangeException();
        }

        if (command.startTime().isBefore(Instant.now())) {
            throw new InvalidAppointmentTimeRangeException();
        }

        if (appointmentRepository.existsActiveAppointmentConflict(
            command.doctorId(),
            command.startTime(),
            command.endTime()
        )) {
            throw new AppointmentTimeConflictException();
        }
    }

}
