package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.domain.prescription.exception.UnauthorizedPrescriptionCancellationException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.CancelPrescriptionCommand;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.CancelPrescriptionUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelPrescriptionService implements CancelPrescriptionUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionWarningLogRepository warningLogRepository;
    private final PrescriptionClinicalContextValidator clinicalContextValidator;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final PrescriptionAccessDeniedAuditWriter accessDeniedAuditWriter;
    private final PrescriptionResultMapper resultMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PrescriptionResult cancel(CancelPrescriptionCommand command) {
        if (command == null || command.prescriptionId() == null) {
            throw new ValidationException("Prescription id is required.");
        }
        if (command.cancelReason() == null || command.cancelReason().isBlank()) {
            throw new ValidationException("Cancellation reason is required.");
        }
        if (!currentUserPort.hasRole("DOCTOR")) {
            throw new AccessDeniedException("Only doctors can cancel prescriptions.");
        }
        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        var prescription = prescriptionRepository.findByIdForUpdate(command.prescriptionId())
                .orElseThrow(() -> new PrescriptionNotFoundException(command.prescriptionId()));
        if (!Objects.equals(prescription.getPrescribedBy(), actorId)) {
            accessDeniedAuditWriter.writeCancelDenied(
                    actorId,
                    prescription.getId(),
                    prescription.getPrescribedBy(),
                    now,
                    "Attempted to cancel prescription prescribed by another doctor"
            );
            throw new UnauthorizedPrescriptionCancellationException();
        }
        clinicalContextValidator.requireDoctorPermissionForPrescriptionCancellation(
                prescription.getMedicalRecordId(),
                actorId
        );
        String reason = command.cancelReason().trim();
        prescription.cancel(reason, actorId, now);
        var saved = prescriptionRepository.save(prescription);
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CANCEL,
                ResourceType.PRESCRIPTION,
                saved.getId(),
                buildAuditDetail(saved, reason, now),
                null,
                now
        ));
        return resultMapper.toResult(
                saved,
                warningLogRepository.findByPrescriptionId(saved.getId())
        );
    }

    private String buildAuditDetail(Prescription prescription, String cancelReason, Instant cancelledAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("prescriptionCode", prescription.getPrescriptionCode());
        detail.put("cancelReason", cancelReason);
        detail.put("cancelledAt", cancelledAt.toString());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize prescription cancellation audit detail.", exception);
        }
    }
}
