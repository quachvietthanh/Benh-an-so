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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentInvalidStatusException;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.appointment.exception.AppointmentPastCutoffException;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.DoctorScheduleNotFoundException;
import com.benhsoan.domain.appointment.exception.DoctorUnavailableException;
import com.benhsoan.domain.appointment.exception.InvalidAppointmentTimeException;
import com.benhsoan.domain.appointment.exception.SlotAlreadyBookedException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.PatientRescheduleAppointmentCommand;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.inbound.appointment.PatientRescheduleAppointmentUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.DoctorScheduleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-004 CV-02: patient self-rescheduling via the online portal. Enforces
 * cross-patient ownership (TC-03 / QTN-23), past-time cutoff (TC-02), slot
 * alignment/working-hours validation and doctor-row pessimistic locking for
 * overlap checking (QTN-04), and writes a RESCHEDULE audit trail (TC-04).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PatientRescheduleAppointmentService implements PatientRescheduleAppointmentUseCase {

    private static final Duration SLOT_DURATION = Duration.ofMinutes(30);

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final String ONLINE_PORTAL = "ONLINE_PORTAL";

    private static final String DEFAULT_RESCHEDULE_REASON = "Đổi lịch hẹn trực tuyến";

    private final AppointmentRepository appointmentRepository;

    private final DoctorScheduleRepository doctorScheduleRepository;

    private final UserRepository userRepository;

    private final CurrentUserPort currentUserPort;

    private final PatientAccessGuard patientAccessGuard;

    private final AuditLogRepository auditLogRepository;

    private final ClockPort clockPort;

    private final PatientAppointmentResultMapper resultMapper;

    private final ObjectMapper objectMapper;

    @Override
    public PatientAppointmentResult reschedule(
            UUID appointmentId,
            PatientRescheduleAppointmentCommand command
    ) {
        if (command.newAppointmentDate() == null || command.newStartTime() == null) {
            throw new ValidationException("newAppointmentDate and newStartTime are required.");
        }

        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        // TC-03 / QTN-23: reject cross-patient access (403) and record ACCESS_DENIED audit.
        patientAccessGuard.requirePatientOwnership(appointment.getPatientId());

        Instant now = clockPort.now();

        // TC-02: the current appointment must still be in the future.
        if (!appointment.getStartTime().isAfter(now)) {
            throw new AppointmentPastCutoffException();
        }

        // Only future SCHEDULED/CONFIRMED appointments can be self-rescheduled.
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new AppointmentInvalidStatusException(
                    "Chỉ có thể đổi lịch hẹn ở trạng thái SCHEDULED hoặc CONFIRMED.");
        }

        Instant oldStartTime = appointment.getStartTime();
        Instant newStartTime = toInstant(command.newAppointmentDate(), command.newStartTime());
        Instant newEndTime = newStartTime.plus(SLOT_DURATION);

        if (!isAlignedToSlot(command.newStartTime())) {
            throw new InvalidAppointmentTimeException("Khung giờ đặt lịch phải theo mốc 30 phút.");
        }
        if (!newStartTime.isAfter(now)) {
            throw new InvalidAppointmentTimeException();
        }

        UUID doctorId = appointment.getDoctorId();

        // QTN-04: lock the doctor's user row so portal and reception serialize per doctor.
        User doctor = userRepository.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));
        if (!doctor.isActive()) {
            throw new DoctorInactiveException(doctor.getId());
        }

        DoctorSchedule schedule = doctorScheduleRepository
                .findByDoctorIdAndScheduleDateForUpdate(doctorId, command.newAppointmentDate())
                .orElseThrow(() -> new DoctorScheduleNotFoundException(doctorId, command.newAppointmentDate()));
        if (!schedule.isActive()) {
            throw new DoctorUnavailableException(doctorId, command.newAppointmentDate());
        }

        LocalTime slotEndTime = command.newStartTime().plus(SLOT_DURATION);
        if (command.newStartTime().isBefore(schedule.getStartTime())
                || slotEndTime.isAfter(schedule.getEndTime())) {
            throw new InvalidAppointmentTimeException("Khung giờ đặt lịch nằm ngoài giờ làm việc của bác sĩ.");
        }

        boolean conflict = appointmentRepository.findActiveAppointmentsForDoctorBetween(
                        doctorId,
                        newStartTime,
                        newEndTime
                ).stream()
                .anyMatch(active -> !active.getId().equals(appointment.getId()));
        if (conflict) {
            throw new SlotAlreadyBookedException();
        }

        String reason = command.reason() == null || command.reason().isBlank()
                ? DEFAULT_RESCHEDULE_REASON
                : command.reason();

        appointment.reschedule(doctorId, newStartTime, newEndTime, reason);
        Appointment saved = appointmentRepository.save(appointment);

        auditLogRepository.save(AuditLog.create(
                currentUserPort.getCurrentUserId(),
                ActionType.UPDATE,
                ResourceType.APPOINTMENT,
                saved.getId(),
                auditDetail(oldStartTime, newStartTime, now),
                null,
                now
        ));

        return resultMapper.toResult(saved);
    }

    private Instant toInstant(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, CLINIC_ZONE).toInstant();
    }

    private boolean isAlignedToSlot(LocalTime startTime) {
        return startTime.getMinute() % 30 == 0
                && startTime.getSecond() == 0
                && startTime.getNano() == 0;
    }

    private String auditDetail(Instant oldStartTime, Instant newStartTime, Instant rescheduledAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", "RESCHEDULE");
        detail.put("channel", ONLINE_PORTAL);
        detail.put("oldAppointmentTime", oldStartTime.toString());
        detail.put("newAppointmentTime", newStartTime.toString());
        detail.put("rescheduledAt", rescheduledAt.toString());

        return toJson(detail);
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize appointment reschedule audit detail.", exception);
        }
    }
}
