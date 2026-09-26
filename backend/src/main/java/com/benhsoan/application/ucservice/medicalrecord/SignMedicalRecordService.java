package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordMissingDiagnosisException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordUnauthorizedSignerException;
import com.benhsoan.domain.medicalrecord.exception.PendingClinicalOrdersWarningException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.SignMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.SignMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SignMedicalRecordService implements SignMedicalRecordUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    private final MedicalRecordTemplateRepository medicalRecordTemplateRepository;
    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordTemplateApplicationMapper templateMapper;
    private final MedicalRecordResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordResult sign(UUID medicalRecordId, SignMedicalRecordCommand command) {
        UUID userId = authorizationService.requireWriteAccess();

        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));

        if (record.isContentLocked()) {
            throw new MedicalRecordAlreadyLockedException();
        }

        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));

        if (visit.isCancelled() || (!visit.isActive() && !visit.isCompleted())) {
            throw new MedicalRecordInvalidVisitException(visit.getId());
        }

        Instant now = clockPort.now();

        if (!visit.getDoctorId().equals(userId)) {
            accessAuditService.recordRecordAccessInNewTransaction(
                    visit.getPatientId(),
                    visit.getId(),
                    record.getId(),
                    userId,
                    MedicalRecordAccessAction.SIGN,
                    "Signature rejected: User is not doctor in charge",
                    now
            );
            throw new MedicalRecordUnauthorizedSignerException(medicalRecordId, visit.getDoctorId());
        }

        boolean hasDiagnosis = medicalRecordDiagnosisRepository.existsByMedicalRecordId(record.getId());
        if (!hasDiagnosis) {
            throw new MedicalRecordMissingDiagnosisException(record.getId());
        }

        if (record.getAppliedTemplateVersionId() != null) {
            MedicalRecordTemplateVersion appliedVersion = medicalRecordTemplateRepository
                    .findVersionById(record.getAppliedTemplateVersionId())
                    .orElseThrow(() -> new MedicalRecordTemplateNotFoundException(record.getAppliedTemplateVersionId()));
            record.ensureRequiredTemplateSections(appliedVersion);
        }

        // QTN-17: Check for pending clinical orders awaiting results
        long pendingOrdersCount = clinicalOrderItemRepository.countPendingByVisitId(visit.getId());
        if (pendingOrdersCount > 0) {
            boolean acknowledged = command != null && Boolean.TRUE.equals(command.acknowledgePendingOrders());
            if (!acknowledged) {
                List<String> pendingServices = clinicalOrderItemRepository.findPendingServiceNamesByVisitId(visit.getId());
                accessAuditService.recordRecordAccessInNewTransaction(
                        visit.getPatientId(),
                        visit.getId(),
                        record.getId(),
                        userId,
                        MedicalRecordAccessAction.SIGN,
                        "Signature blocked: Pending paraclinical orders waiting for results: " + String.join(", ", pendingServices),
                        now
                );
                throw new PendingClinicalOrdersWarningException(pendingServices);
            }
        }

        String signatureData = (command != null && command.signatureData() != null && !command.signatureData().isBlank())
                ? command.signatureData().trim()
                : "SIMULATED_SIGNATURE:" + userId + ":" + now.toEpochMilli();

        record.sign(signatureData, userId, now);
        MedicalRecord saved = medicalRecordRepository.save(record);

        String auditDetail = pendingOrdersCount > 0
                ? "Medical record signed (acknowledged pending paraclinical orders)"
                : "Medical record signed";
        if (visit.getInitialDoctorId() != null && !visit.getInitialDoctorId().equals(userId)) {
            auditDetail += " (handed over from initial doctor: " + visit.getInitialDoctorId() + ")";
        }

        accessAuditService.recordRecordAccess(
                visit.getPatientId(),
                visit.getId(),
                saved.getId(),
                userId,
                MedicalRecordAccessAction.SIGN,
                auditDetail,
                now
        );

        return resultMapper.toResult(saved, templateMapper.resolveApplied(saved, visit));
    }
}
