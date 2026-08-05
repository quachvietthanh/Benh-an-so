package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.CancelPrescriptionUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

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
    private final PrescriptionResultMapper resultMapper;

    @Override
    public PrescriptionResult cancel(UUID prescriptionId) {
        if (!currentUserPort.hasRole("DOCTOR")) {
            throw new AccessDeniedException("Only doctors can cancel prescriptions.");
        }
        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        var prescription = prescriptionRepository.findByIdForUpdate(prescriptionId)
                .orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));
        if (!Objects.equals(prescription.getPrescribedBy(), actorId)) {
            throw new AccessDeniedException("Only the prescribing doctor can cancel a prescription.");
        }
        clinicalContextValidator.requireEditableRecordForDoctor(
                prescription.getMedicalRecordId(),
                actorId
        );
        prescription.cancel(actorId, now);
        var saved = prescriptionRepository.save(prescription);
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.CANCEL,
                ResourceType.PRESCRIPTION,
                saved.getId(),
                "{\"prescriptionCode\":\"%s\"}".formatted(saved.getPrescriptionCode()),
                null
        ));
        return resultMapper.toResult(
                saved,
                warningLogRepository.findByPrescriptionId(saved.getId())
        );
    }
}
