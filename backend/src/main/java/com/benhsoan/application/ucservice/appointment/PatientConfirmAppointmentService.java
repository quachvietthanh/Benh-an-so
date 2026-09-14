package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.inbound.appointment.PatientConfirmAppointmentUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-03-CN-008 CV-03: Patient self-confirmation via the online portal.
 * Enforces cross-patient ownership (TC-02 / QTN-23), status precondition (SCHEDULED)
 * and cutoff time (startTime > now). Writes CONFIRM audit trail with ONLINE_PORTAL channel.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PatientConfirmAppointmentService implements PatientConfirmAppointmentUseCase {

    private static final String ONLINE_PORTAL = "ONLINE_PORTAL";

    private final AppointmentRepository appointmentRepository;
    private final CurrentUserPort currentUserPort;
    private final PatientAccessGuard patientAccessGuard;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;
    private final PatientAppointmentResultMapper resultMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PatientAppointmentResult confirm(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        // TC-02 / QTN-23: reject cross-patient access (403) and record ACCESS_DENIED audit.
        patientAccessGuard.requirePatientOwnership(
                appointment.getPatientId(),
                ResourceType.APPOINTMENT,
                appointment.getId()
        );

        Instant now = clockPort.now();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        // Domain method validates status (SCHEDULED) and past cutoff (startTime > now).
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

        return resultMapper.toResult(saved);
    }

    private String auditDetail(Appointment appointment, Instant confirmedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", "CONFIRM");
        detail.put("channel", ONLINE_PORTAL);
        detail.put("appointmentCode", appointment.getAppointmentCode());
        detail.put("confirmedAt", confirmedAt.toString());

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize patient appointment confirmation audit detail.", exception);
        }
    }
}
