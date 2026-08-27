package com.benhsoan.application.ucservice.appointment;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.InvalidAppointmentTimeException;
import com.benhsoan.domain.appointment.exception.SlotAlreadyBookedException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.PatientBookAppointmentCommand;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.inbound.appointment.PatientBookAppointmentUseCase;
import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-003 CV-03: books an appointment on behalf of the authenticated patient via the
 * online portal, guarding past time (TC-03), slot collision (TC-02 / QTN-04) and writing an
 * audit trail (TC-04).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PatientBookAppointmentService implements PatientBookAppointmentUseCase {

    private static final Duration SLOT_DURATION = Duration.ofMinutes(30);

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final String ONLINE_PORTAL = "ONLINE_PORTAL";

    private static final String DEFAULT_REASON = "Đặt lịch hẹn trực tuyến";

    private final AppointmentRepository appointmentRepository;

    private final AppointmentCodeGenerator appointmentCodeGenerator;

    private final DoctorScheduleRepository doctorScheduleRepository;

    private final PatientRepository patientRepository;

    private final UserRepository userRepository;

    private final CurrentUserPort currentUserPort;

    private final AuditLogRepository auditLogRepository;

    private final ClockPort clockPort;

    private final ObjectMapper objectMapper;

    @Override
    public PatientAppointmentResult book(PatientBookAppointmentCommand command) {
        if (command.doctorId() == null
                || command.appointmentDate() == null
                || command.startTime() == null) {
            throw new ValidationException("doctorId, appointmentDate and startTime are required.");
        }

        Instant now = clockPort.now();
        Instant startTime = toInstant(command.appointmentDate(), command.startTime());
        Instant endTime = startTime.plus(SLOT_DURATION);

        if (!startTime.isAfter(now)) {
            throw new InvalidAppointmentTimeException();
        }

        UUID userId = currentUserPort.getCurrentUserId();
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "No patient profile is linked to the authenticated user."));
        UUID patientId = patient.getId();

        User doctor = userRepository.findById(command.doctorId())
                .orElseThrow(() -> new DoctorNotFoundException(command.doctorId()));
        if (!doctor.isActive()) {
            throw new DoctorInactiveException(doctor.getId());
        }

        // QTN-04: pessimistically lock the doctor's schedule row for the date (when present)
        // to serialize concurrent online bookings for the same doctor and date.
        doctorScheduleRepository.findByDoctorIdAndScheduleDateForUpdate(
                command.doctorId(),
                command.appointmentDate()
        );

        if (!appointmentRepository.findActiveAppointmentsForDoctorBetween(
                        command.doctorId(),
                        startTime,
                        endTime
                ).isEmpty()) {
            throw new SlotAlreadyBookedException();
        }

        String reason = command.reason() == null || command.reason().isBlank()
                ? DEFAULT_REASON
                : command.reason();

        String appointmentCode = appointmentCodeGenerator.generate();

        Appointment appointment = Appointment.create(
                appointmentCode,
                patientId,
                command.doctorId(),
                startTime,
                endTime,
                reason,
                userId,
                ONLINE_PORTAL
        );

        Appointment saved = appointmentRepository.save(appointment);

        auditLogRepository.save(AuditLog.create(
                userId,
                ActionType.CREATE,
                ResourceType.APPOINTMENT,
                saved.getId(),
                auditDetail(saved, now),
                null,
                now
        ));

        return toResult(saved);
    }

    private Instant toInstant(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, CLINIC_ZONE).toInstant();
    }

    private String auditDetail(Appointment appointment, Instant bookedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("patientId", appointment.getPatientId().toString());
        detail.put("doctorId", appointment.getDoctorId().toString());
        detail.put("appointmentTime", appointment.getStartTime().toString());
        detail.put("channel", ONLINE_PORTAL);
        detail.put("bookedAt", bookedAt.toString());

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize appointment booking audit detail.", exception);
        }
    }

    private PatientAppointmentResult toResult(Appointment appointment) {
        return new PatientAppointmentResult(
                appointment.getId(),
                appointment.getAppointmentCode(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getBookingChannel(),
                appointment.getCreatedAt()
        );
    }
}
