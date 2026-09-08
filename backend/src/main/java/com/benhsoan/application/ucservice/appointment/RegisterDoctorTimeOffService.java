package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.ArrayList;
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
import com.benhsoan.domain.appointment.exception.InvalidDoctorRoleException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
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
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-03-CN-006 CV-03 / TC-03: registers a doctor's ad-hoc time-off / leave interval,
 * detecting and listing all existing appointments affected by this break window so
 * receptionists can reschedule or cancel them.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RegisterDoctorTimeOffService implements RegisterDoctorTimeOffUseCase {

    private final DoctorTimeOffRepository doctorTimeOffRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public DoctorTimeOffResult registerTimeOff(RegisterDoctorTimeOffCommand command) {
        if (command == null || command.doctorId() == null || command.startTime() == null || command.endTime() == null) {
            throw new ValidationException("doctorId, startTime and endTime are required.");
        }

        if (!command.endTime().isAfter(command.startTime())) {
            throw new ValidationException("Time-off end time must be after start time.");
        }

        User doctor = userRepository.findById(command.doctorId())
                .orElseThrow(() -> new DoctorNotFoundException(command.doctorId()));
        if (!doctor.isActive()) {
            throw new DoctorInactiveException(doctor.getId());
        }
        requireDoctorRole(doctor);

        Instant now = clockPort.now();
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

        // NCL-03-CN-006-TC-03: detect all active appointments overlapping with this time-off interval
        List<Appointment> conflictingAppointments = appointmentRepository.findActiveAppointmentsForDoctorBetween(
                command.doctorId(),
                command.startTime(),
                command.endTime()
        );

        List<AffectedAppointmentResult> affectedList = new ArrayList<>();
        for (Appointment appt : conflictingAppointments) {
            String patientName = null;
            String patientPhone = null;
            if (appt.getPatientId() != null) {
                Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
                if (patient != null) {
                    patientName = patient.getFullName();
                    patientPhone = patient.getPhone();
                }
            }

            affectedList.add(new AffectedAppointmentResult(
                    appt.getId(),
                    appt.getAppointmentCode(),
                    appt.getPatientId(),
                    patientName,
                    patientPhone,
                    appt.getStartTime(),
                    appt.getEndTime(),
                    appt.getStatus(),
                    appt.getReason()
            ));
        }

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.CREATE,
                ResourceType.DOCTOR_TIME_OFF,
                saved.getId(),
                auditDetail(saved, affectedList.size(), now),
                null,
                now
        ));

        return new DoctorTimeOffResult(
                saved.getId(),
                saved.getDoctorId(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getReason(),
                saved.getStatus(),
                saved.getCreatedBy(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                affectedList
        );
    }

    private void requireDoctorRole(User doctor) {
        Role doctorRole = roleRepository.findByName("DOCTOR")
                .orElseThrow(() -> new IllegalStateException("DOCTOR role is not configured."));
        if (!doctorRole.getId().equals(doctor.getRoleId())) {
            throw new InvalidDoctorRoleException(doctor.getId());
        }
    }

    private String auditDetail(DoctorTimeOff timeOff, int affectedCount, Instant now) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("timeOffId", timeOff.getId().toString());
        detail.put("doctorId", timeOff.getDoctorId().toString());
        detail.put("startTime", timeOff.getStartTime().toString());
        detail.put("endTime", timeOff.getEndTime().toString());
        detail.put("affectedAppointmentsCount", affectedCount);
        detail.put("createdAt", now.toString());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize doctor time-off audit detail.", e);
        }
    }
}
