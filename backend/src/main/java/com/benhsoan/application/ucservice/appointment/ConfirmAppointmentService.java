package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.appointment.exception.UnauthorizedAppointmentOperationException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.inbound.appointment.ConfirmAppointmentUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-03-CN-008: Receptionist confirms appointment at counter or by phone.
 * Preconditions: status is SCHEDULED and not past cutoff (startTime > now).
 * Business Rules: QTN-01 (RBAC), QTN-08.
 * Acceptance Criteria: TC-01, TC-03.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConfirmAppointmentService implements ConfirmAppointmentUseCase {

    private static final String RECEPTION_COUNTER = "RECEPTION_COUNTER";

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final CurrentUserPort currentUserPort;
    private final AuditLogRepository auditLogRepository;
    private final AppointmentAccessDeniedAuditWriter accessDeniedAuditWriter;
    private final ClockPort clockPort;
    private final AppointmentResultMapper resultMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AppointmentResult confirm(UUID appointmentId) {
        if (!currentUserPort.hasRole("RECEPTIONIST") && !currentUserPort.hasRole("ADMIN")) {
            accessDeniedAuditWriter.writeConfirmDenied(
                    currentUserPort.getCurrentUserId(),
                    appointmentId,
                    clockPort.now(),
                    "User lacks RECEPTIONIST or ADMIN role to confirm appointment"
            );
            throw new UnauthorizedAppointmentOperationException();
        }

        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        Instant now = clockPort.now();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        appointment.confirm(currentUserId, now);
        Appointment saved = appointmentRepository.save(appointment);

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.UPDATE,
                ResourceType.APPOINTMENT,
                saved.getId(),
                auditDetail(saved, now),
                null,
                now
        ));

        String confirmedByName = userRepository.findById(currentUserId)
                .map(User::getFullName)
                .orElse("Unknown");

        return resultMapper.toResult(saved, List.of(), confirmedByName);
    }

    private String auditDetail(Appointment appointment, Instant confirmedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", "CONFIRM");
        detail.put("channel", RECEPTION_COUNTER);
        detail.put("appointmentCode", appointment.getAppointmentCode());
        detail.put("confirmedAt", confirmedAt.toString());

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize appointment confirmation audit detail.", exception);
        }
    }
}
