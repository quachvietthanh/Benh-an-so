package com.benhsoan.application.ucservice.portal;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.MedicalAttachment;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultSummaryResult;
import com.benhsoan.port.inbound.portal.GetPatientPortalClinicalResultsUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.clinical.MedicalAttachmentRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-009 CV-02: Returns confirmed clinical results for a given visit on the patient portal.
 * Strictly enforces QTN-23 via {@link PatientAccessGuard}, denying cross-patient access (TC-03),
 * filters only FINAL results (TC-02), supports results completed after visit ended (TC-04),
 * and audits READ operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetPatientPortalClinicalResultsService implements GetPatientPortalClinicalResultsUseCase {

    private static final String ONLINE_PORTAL = "ONLINE_PORTAL";

    private final VisitRepository visitRepository;
    private final ClinicalResultRepository clinicalResultRepository;
    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final MedicalAttachmentRepository medicalAttachmentRepository;
    private final UserRepository userRepository;
    private final PatientAccessGuard patientAccessGuard;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public List<PatientPortalClinicalResultSummaryResult> getClinicalResults(UUID visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));

        // TC-03 / QTN-23: reject cross-patient access (403) and record ACCESS_DENIED audit log
        patientAccessGuard.requirePatientOwnership(
                visit.getPatientId(),
                ResourceType.CLINICAL_RESULT,
                visitId
        );

        // TC-02: only confirmed (FINAL) results are visible to the patient
        // TC-04: supported even if visit is COMPLETED
        List<ClinicalResult> results = clinicalResultRepository.findByVisitIdAndStatus(visitId, ClinicalResultStatus.FINAL);

        if (results.isEmpty()) {
            return List.of();
        }

        List<UUID> resultIds = results.stream().map(ClinicalResult::getId).toList();
        List<MedicalAttachment> attachments = medicalAttachmentRepository.findByClinicalResultIdIn(resultIds);
        Set<UUID> resultsWithAttachments = attachments.stream()
                .map(MedicalAttachment::getClinicalResultId)
                .collect(Collectors.toSet());

        Instant viewedAt = clockPort.now();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        // Audit READ operation
        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.READ,
                ResourceType.CLINICAL_RESULT,
                visit.getId(),
                auditDetail(visit, results.size(), viewedAt),
                null,
                viewedAt
        ));

        List<UUID> itemIds = results.stream().map(ClinicalResult::getClinicalOrderItemId).distinct().toList();
        Map<UUID, ClinicalOrderItem> itemMap = clinicalOrderItemRepository.findByIdIn(itemIds).stream()
                .collect(Collectors.toMap(ClinicalOrderItem::getId, Function.identity(), (a, b) -> a));

        List<UUID> doctorIds = results.stream().map(ClinicalResult::getEnteredBy).distinct().toList();
        Map<UUID, User> doctorMap = userRepository.findAllById(doctorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        return results.stream().map(result -> {
            ClinicalOrderItem item = itemMap.get(result.getClinicalOrderItemId());
            User doctor = doctorMap.get(result.getEnteredBy());
            String doctorName = doctor != null ? doctor.getFullName() : null;

            return new PatientPortalClinicalResultSummaryResult(
                    result.getId(),
                    result.getClinicalOrderItemId(),
                    result.getVisitId(),
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
                    doctorName,
                    result.getEnteredAt(),
                    resultsWithAttachments.contains(result.getId())
            );
        }).toList();
    }

    private String auditDetail(Visit visit, int count, Instant viewedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("channel", ONLINE_PORTAL);
        detail.put("visitId", visit.getId().toString());
        detail.put("patientId", visit.getPatientId().toString());
        detail.put("resultCount", count);
        detail.put("viewedAt", viewedAt.toString());

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize patient clinical results audit.", exception);
        }
    }
}
