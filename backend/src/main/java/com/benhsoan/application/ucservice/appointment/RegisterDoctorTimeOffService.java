package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.DoctorTimeOffConflictException;
import com.benhsoan.domain.appointment.exception.InvalidDoctorRoleException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.RegisterDoctorTimeOffCommand;
import com.benhsoan.port.dto.result.appointment.AffectedAppointmentResult;
import com.benhsoan.port.dto.result.appointment.DoctorTimeOffResult;
import com.benhsoan.port.inbound.appointment.RegisterDoctorTimeOffUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorTimeOffRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-03-CN-006 / QTN-30: Registers an unexpected time-off interval for a doctor.
 * Pessimistically locks the doctor to serialize against concurrent bookings.
 * Finds and returns any existing active appointments overlapping this interval (TC-03).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RegisterDoctorTimeOffService implements RegisterDoctorTimeOffUseCase {

    private final DoctorTimeOffRepository doctorTimeOffRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public DoctorTimeOffResult registerTimeOff(RegisterDoctorTimeOffCommand command) {
        Guard.require(command, "Command");
        Guard.require(command.doctorId(), "Doctor id");
        Guard.require(command.startTime(), "Start time");
        Guard.require(command.endTime(), "End time");
        Guard.require(command.reason(), "Reason");

        Instant now = clockPort.now();
        if (!command.endTime().isAfter(command.startTime())) {
            throw new ValidationException("Thời gian kết thúc nghỉ phải sau thời gian bắt đầu.");
        }
        if (command.startTime().isBefore(now)) {
            throw new ValidationException("Thời gian nghỉ không được bắt đầu trong quá khứ.");
        }

        // Pessimistic lock on doctor user row to serialize with concurrent appointment booking
        User doctor = userRepository.findByIdForUpdate(command.doctorId())
                .orElseThrow(() -> new DoctorNotFoundException(command.doctorId()));
        if (!doctor.isActive()) {
            throw new DoctorInactiveException(doctor.getId());
        }
        requireDoctorRole(doctor);

        if (doctorTimeOffRepository.existsActiveOverlapping(command.doctorId(), command.startTime(), command.endTime())) {
            throw new DoctorTimeOffConflictException();
        }

        UUID currentUserId = currentUserPort.getCurrentUserId();

        DoctorTimeOff timeOff = DoctorTimeOff.create(
                command.doctorId(),
                command.startTime(),
                command.endTime(),
                command.reason(),
                currentUserId,
                now
        );

        DoctorTimeOff saved = doctorTimeOffRepository.save(timeOff);

        // TC-03: List affected active appointments so receptionist can reschedule or cancel
        List<Appointment> affectedAppointments = appointmentRepository.findActiveAppointmentsForDoctorBetween(
                command.doctorId(),
                command.startTime(),
                command.endTime()
        );

        List<AffectedAppointmentResult> affectedResults = affectedAppointments.stream()
                .map(this::toAffectedResult)
                .toList();

        // Audit log (QTN-31)
        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.CREATE,
                ResourceType.DOCTOR_TIMEOFF,
                saved.getId(),
                buildAuditDetail(saved, affectedResults.size(), now),
                null,
                now
        ));

        return toResult(saved, affectedResults);
    }

    private void requireDoctorRole(User doctor) {
        Role doctorRole = roleRepository.findByName("DOCTOR")
                .orElseThrow(() -> new IllegalStateException("DOCTOR role is not configured."));
        if (!doctorRole.getId().equals(doctor.getRoleId())) {
            throw new InvalidDoctorRoleException(doctor.getId());
        }
    }

    private String buildAuditDetail(DoctorTimeOff timeOff, int affectedCount, Instant now) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("doctorId", timeOff.getDoctorId().toString());
        detail.put("startTime", timeOff.getStartTime().toString());
        detail.put("endTime", timeOff.getEndTime().toString());
        detail.put("reason", timeOff.getReason());
        detail.put("affectedAppointmentsCount", affectedCount);
        detail.put("createdAt", now.toString());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{\"timeOffId\":\"" + timeOff.getId() + "\"}";
        }
    }

    private AffectedAppointmentResult toAffectedResult(Appointment appt) {
        return new AffectedAppointmentResult(
                appt.getId(),
                appt.getAppointmentCode(),
                appt.getPatientId(),
                appt.getStartTime(),
                appt.getEndTime(),
                appt.getStatus(),
                appt.getReason()
        );
    }

    private DoctorTimeOffResult toResult(DoctorTimeOff timeOff, List<AffectedAppointmentResult> affectedAppointments) {
        return new DoctorTimeOffResult(
                timeOff.getId(),
                timeOff.getDoctorId(),
                timeOff.getStartTime(),
                timeOff.getEndTime(),
                timeOff.getReason(),
                timeOff.getStatus(),
                timeOff.getCreatedBy(),
                timeOff.getCreatedAt(),
                affectedAppointments
        );
    }
}
