package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.AppointmentRescheduleLog;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.appointment.exception.AppointmentTimeConflictException;
import com.benhsoan.domain.appointment.exception.AppointmentTimeInPastException;
import com.benhsoan.domain.appointment.exception.DoctorInactiveException;
import com.benhsoan.domain.appointment.exception.DoctorNotFoundException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.portal.notification.AppointmentChangedNotificationRequested;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.appointment.RescheduleAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.dto.result.appointment.AppointmentRescheduleHistoryResult;
import com.benhsoan.port.inbound.appointment.RescheduleAppointmentUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRescheduleLogRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-03-CN-007: Receptionist counter reschedule use case service.
 * Enforces role authorization (APPOINTMENT_UPDATE / RECEPTIONIST, ADMIN),
 * past-cutoff / status preconditions (TC-02), doctor working hours & time-off (QTN-30),
 * overlap checking with pessimistic lock (QTN-04, TC-03), persists reschedule history (TC-04),
 * and writes audit logs.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RescheduleAppointmentService implements RescheduleAppointmentUseCase {

    private static final String RECEPTION_COUNTER = "RECEPTION_COUNTER";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentRescheduleLogRepository rescheduleLogRepository;
    private final UserRepository userRepository;
    private final DoctorScheduleValidator doctorScheduleValidator;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final AppointmentResultMapper resultMapper;
    private final AppointmentRescheduleHistoryAssembler historyAssembler;
    private final AppointmentAccessDeniedAuditWriter accessDeniedAuditWriter;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public AppointmentResult reschedule(UUID appointmentId, RescheduleAppointmentCommand command) {
        validateCommand(command);

        if (!currentUserPort.hasRole("RECEPTIONIST") && !currentUserPort.hasRole("ADMIN")) {
            accessDeniedAuditWriter.writeRescheduleDenied(
                    currentUserPort.getCurrentUserId(),
                    appointmentId,
                    clockPort.now(),
                    "User lacks RECEPTIONIST or ADMIN role to reschedule appointment"
            );
            throw new UnauthorizedAppointmentOperationException();
        }

        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        Instant now = clockPort.now();
        if (command.startTime().isBefore(now)) {
            throw new AppointmentTimeInPastException();
        }

        UUID targetDoctorId = command.newDoctorId() != null
                ? command.newDoctorId()
                : appointment.getDoctorId();

        User doctor = userRepository.findByIdForUpdate(targetDoctorId)
                .orElseThrow(() -> new DoctorNotFoundException(targetDoctorId));
        if (!doctor.isActive()) {
            throw new DoctorInactiveException(doctor.getId());
        }

        // QTN-30: Verify target doctor is working and available (not on time-off)
        doctorScheduleValidator.validateDoctorWorkingAndAvailable(
                targetDoctorId,
                command.startTime(),
                command.endTime()
        );

        // QTN-04 / TC-03: Overlap conflict check (excluding current appointment if same doctor)
        boolean conflict = appointmentRepository.findActiveAppointmentsForDoctorBetween(
                        targetDoctorId,
                        command.startTime(),
                        command.endTime()
                ).stream()
                .anyMatch(active -> !active.getId().equals(appointment.getId()));

        if (conflict) {
            throw new AppointmentTimeConflictException();
        }

        UUID oldDoctorId = appointment.getDoctorId();
        Instant oldStartTime = appointment.getStartTime();
        Instant oldEndTime = appointment.getEndTime();
        String trimmedReason = command.reason().trim();

        // Domain method validates status (SCHEDULED/CONFIRMED) and past cutoff; preserves clinical reason
        appointment.reschedule(targetDoctorId, command.startTime(), command.endTime(), now);
        Appointment saved = appointmentRepository.save(appointment);

        UUID currentUserId = currentUserPort.getCurrentUserId();

        // TC-04: Persist structured reschedule history log
        AppointmentRescheduleLog rescheduleLog = AppointmentRescheduleLog.create(
                saved.getId(),
                oldDoctorId,
                targetDoctorId,
                oldStartTime,
                oldEndTime,
                command.startTime(),
                command.endTime(),
                trimmedReason,
                currentUserId,
                now
        );
        rescheduleLogRepository.save(rescheduleLog);

        applicationEventPublisher.publishEvent(
                new AppointmentChangedNotificationRequested(saved, rescheduleLog, now));

        // General system audit log
        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.UPDATE,
                ResourceType.APPOINTMENT,
                saved.getId(),
                auditDetail(oldDoctorId, targetDoctorId, oldStartTime, oldEndTime, command.startTime(), command.endTime(), trimmedReason, now),
                null,
                now
        ));

        List<AppointmentRescheduleHistoryResult> histories = historyAssembler.getHistoriesForAppointment(saved.getId());
        return resultMapper.toResult(saved, histories);
    }

    private void validateCommand(RescheduleAppointmentCommand command) {
        if (command == null) {
            throw new ValidationException("Command cannot be null.");
        }
        if (command.startTime() == null || command.endTime() == null) {
            throw new ValidationException("Start time and end time are required.");
        }
        if (!command.endTime().isAfter(command.startTime())) {
            throw new ValidationException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new ValidationException("Lý do dời lịch là bắt buộc.");
        }
    }

    private String auditDetail(
            UUID oldDoctorId,
            UUID newDoctorId,
            Instant oldStartTime,
            Instant oldEndTime,
            Instant newStartTime,
            Instant newEndTime,
            String reason,
            Instant rescheduledAt
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", "RESCHEDULE");
        detail.put("channel", RECEPTION_COUNTER);
        detail.put("oldDoctorId", oldDoctorId.toString());
        detail.put("newDoctorId", newDoctorId.toString());
        detail.put("oldStartTime", oldStartTime.toString());
        detail.put("oldEndTime", oldEndTime.toString());
        detail.put("newStartTime", newStartTime.toString());
        detail.put("newEndTime", newEndTime.toString());
        detail.put("reason", reason);
        detail.put("rescheduledAt", rescheduledAt.toString());

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize reschedule audit detail.", e);
        }
    }
}
