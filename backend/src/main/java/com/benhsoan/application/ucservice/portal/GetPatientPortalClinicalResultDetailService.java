package com.benhsoan.application.ucservice.portal;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.exception.ClinicalResultNotFoundException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultDetailResult;
import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultDetailResult.AttachmentView;
import com.benhsoan.port.inbound.portal.GetPatientPortalClinicalResultDetailUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.clinical.MedicalAttachmentRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-009 CV-02: Returns single clinical result detail for the authenticated patient.
 * Strictly enforces QTN-23 via {@link PatientAccessGuard}, denying cross-patient access (TC-03),
 * hides unconfirmed (DRAFT) results (TC-02), and audits READ operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetPatientPortalClinicalResultDetailService implements GetPatientPortalClinicalResultDetailUseCase {

    private static final String ONLINE_PORTAL = "ONLINE_PORTAL";

    private final ClinicalResultRepository clinicalResultRepository;
    private final VisitRepository visitRepository;
    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final MedicalAttachmentRepository medicalAttachmentRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PatientAccessGuard patientAccessGuard;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public PatientPortalClinicalResultDetailResult getClinicalResultDetail(UUID clinicalResultId) {
        ClinicalResult result = clinicalResultRepository.findById(clinicalResultId)
                .orElseThrow(() -> new ClinicalResultNotFoundException(clinicalResultId));

        Visit visit = visitRepository.findById(result.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(result.getVisitId()));

        // TC-03 / QTN-23: reject cross-patient access (403) and record ACCESS_DENIED audit log
        patientAccessGuard.requirePatientOwnership(
                visit.getPatientId(),
                ResourceType.CLINICAL_RESULT,
                clinicalResultId
        );

        // TC-02: unconfirmed (DRAFT) results are hidden from patient
        if (result.getStatus() != ClinicalResultStatus.FINAL) {
            throw new ClinicalResultNotFoundException(clinicalResultId);
        }

        ClinicalOrderItem item = clinicalOrderItemRepository.findById(result.getClinicalOrderItemId()).orElse(null);

        String orderingDoctorName = null;
        if (item != null) {
            ClinicalOrder order = clinicalOrderRepository.findById(item.getClinicalOrderId()).orElse(null);
            if (order != null) {
                orderingDoctorName = userRepository.findById(order.getOrderedBy())
                        .map(User::getFullName)
                        .orElse(null);
            }
        }

        String performingDoctorName = userRepository.findById(result.getEnteredBy())
                .map(User::getFullName)
                .orElse(null);

        String specialtyName = specialtyRepository.findById(visit.getSpecialtyId())
                .map(Specialty::getName)
                .orElse(null);

        List<AttachmentView> attachments = medicalAttachmentRepository.findByClinicalResultId(result.getId()).stream()
                .map(att -> new AttachmentView(
                        att.getId(),
                        att.getFileName(),
                        att.getContentType(),
                        att.getFileSize(),
                        att.getAttachmentType() != null ? att.getAttachmentType().name() : null
                ))
                .toList();

        Instant viewedAt = clockPort.now();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        // Audit READ operation
        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.READ,
                ResourceType.CLINICAL_RESULT,
                result.getId(),
                auditDetail(visit, result, viewedAt),
                null,
                viewedAt
        ));

        return new PatientPortalClinicalResultDetailResult(
                result.getId(),
                result.getClinicalOrderItemId(),
                visit.getId(),
                visit.getVisitCode(),
                visit.getVisitAt(),
                item != null ? item.getServiceCode() : null,
                item != null ? item.getServiceName() : null,
                result.getResultType() != null ? result.getResultType().name() : null,
                result.getNumericValue(),
                result.getLowerBound(),
                result.getUpperBound(),
                result.getTextValue(),
                result.getUnit(),
                result.getReferenceRange(),
                result.getAbnormalFlag() != null ? result.getAbnormalFlag().name() : null,
                result.getConclusion(),
                result.getStatus() != null ? result.getStatus().name() : null,
                orderingDoctorName,
                performingDoctorName,
                specialtyName,
                result.getEnteredAt(),
                attachments
        );
    }

    private String auditDetail(Visit visit, ClinicalResult result, Instant viewedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("channel", ONLINE_PORTAL);
        detail.put("clinicalResultId", result.getId().toString());
        detail.put("visitId", visit.getId().toString());
        detail.put("patientId", visit.getPatientId().toString());
        detail.put("viewedAt", viewedAt.toString());

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize patient clinical result detail audit.", exception);
        }
    }
}
