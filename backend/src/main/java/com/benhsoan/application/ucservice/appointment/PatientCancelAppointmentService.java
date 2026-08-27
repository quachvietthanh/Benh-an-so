package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentInvalidStatusException;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.appointment.exception.AppointmentPastCutoffException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.dto.command.appointment.PatientCancelAppointmentCommand;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;
import com.benhsoan.port.inbound.appointment.PatientCancelAppointmentUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-004 CV-02: patient self-cancellation via the online portal. Enforces
 * cross-patient ownership (TC-03 / QTN-23), past-time cutoff (TC-02), status
 * lifecycle (TC-01) and writes a CANCEL audit trail (TC-04).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PatientCancelAppointmentService implements PatientCancelAppointmentUseCase {

    private static final String ONLINE_PORTAL = "ONLINE_PORTAL";

    private static final String DEFAULT_CANCEL_REASON = "Hủy lịch hẹn trực tuyến";

    private final AppointmentRepository appointmentRepository;

    private final CurrentUserPort currentUserPort;

    private final PatientAccessGuard patientAccessGuard;

    private final AuditLogRepository auditLogRepository;

    private final ClockPort clockPort;

    private final PatientAppointmentResultMapper resultMapper;

    private final ObjectMapper objectMapper;

    @Override
    public PatientAppointmentResult cancel(
            UUID appointmentId,
            PatientCancelAppointmentCommand command
    ) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        // TC-03 / QTN-23: reject cross-patient access (403) and record ACCESS_DENIED audit.
        patientAccessGuard.requirePatientOwnership(
                appointment.getPatientId(),
                ResourceType.APPOINTMENT,
                appointment.getId());

        Instant now = clockPort.now();

        // TC-02: past-time cutoff.
        if (!appointment.getStartTime().isAfter(now)) {
            throw new AppointmentPastCutoffException();
        }

        // TC-01: only future SCHEDULED/CONFIRMED appointments can be self-cancelled.
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new AppointmentInvalidStatusException(
                    "Chỉ có thể hủy lịch hẹn ở trạng thái SCHEDULED hoặc CONFIRMED.");
        }

        String cancelReason = command.cancellationReason() == null || command.cancellationReason().isBlank()
                ? DEFAULT_CANCEL_REASON
                : command.cancellationReason();

        appointment.cancel(cancelReason);
        Appointment saved = appointmentRepository.save(appointment);

        auditLogRepository.save(AuditLog.create(
                currentUserPort.getCurrentUserId(),
                ActionType.CANCEL,
                ResourceType.APPOINTMENT,
                saved.getId(),
                auditDetail(saved, cancelReason, now),
                null,
                now
        ));

        return resultMapper.toResult(saved);
    }

    private String auditDetail(Appointment appointment, String cancelReason, Instant cancelledAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", "CANCEL");
        detail.put("channel", ONLINE_PORTAL);
        detail.put("appointmentTime", appointment.getStartTime().toString());
        detail.put("cancelReason", cancelReason);
        detail.put("cancelledAt", cancelledAt.toString());

        return toJson(detail);
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize appointment cancellation audit detail.", exception);
        }
    }
}
