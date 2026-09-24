package com.benhsoan.application.ucservice.portal;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.clinical.exception.ClinicalResultNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.portal.ClinicalResultPrintDocument;
import com.benhsoan.port.dto.result.portal.ClinicalResultPrintDocument.ClinicalResultPrintItem;
import com.benhsoan.port.dto.result.portal.ClinicalResultPrintResult;
import com.benhsoan.port.inbound.portal.ExportPatientPortalClinicalResultUseCase;
import com.benhsoan.port.outbound.pdf.ClinicalResultPdfRenderer;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-009 CV-02: Generates a readable PDF document for clinical results requested
 * by the patient on the portal (TC-01), verifying QTN-23 ownership (TC-03), filtering only
 * confirmed (FINAL) results (TC-02), and recording EXPORT audit event.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ExportPatientPortalClinicalResultService implements ExportPatientPortalClinicalResultUseCase {

    private static final String ONLINE_PORTAL = "ONLINE_PORTAL";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClinicalResultRepository clinicalResultRepository;
    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final ClinicalOrderRepository clinicalOrderRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final ClinicalResultPdfRenderer clinicalResultPdfRenderer;
    private final PatientAccessGuard patientAccessGuard;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public ClinicalResultPrintResult exportByResult(UUID clinicalResultId) {
        ClinicalResult result = clinicalResultRepository.findById(clinicalResultId)
                .orElseThrow(() -> new ClinicalResultNotFoundException(clinicalResultId));

        Visit visit = visitRepository.findById(result.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(result.getVisitId()));

        patientAccessGuard.requirePatientOwnership(
                visit.getPatientId(),
                ResourceType.CLINICAL_RESULT,
                clinicalResultId
        );

        if (result.getStatus() != ClinicalResultStatus.FINAL) {
            throw new ClinicalResultNotFoundException(clinicalResultId);
        }

        Patient patient = patientRepository.findById(visit.getPatientId())
                .orElseThrow(() -> new PatientNotFoundException(visit.getPatientId()));

        ClinicConfiguration clinic = clinicConfigurationRepository.find().orElse(null);
        String clinicName = clinic != null ? clinic.getClinicName() : "Phòng khám Đa khoa";
        String clinicAddress = clinic != null ? clinic.getAddress() : "-";
        String clinicPhone = clinic != null ? clinic.getPhone() : "-";

        ClinicalOrderItem item = clinicalOrderItemRepository.findById(result.getClinicalOrderItemId()).orElse(null);
        String orderCode = null;
        String clinicalReason = null;
        String doctorName = null;

        if (item != null) {
            ClinicalOrder order = clinicalOrderRepository.findById(item.getClinicalOrderId()).orElse(null);
            if (order != null) {
                orderCode = order.getOrderCode();
                clinicalReason = order.getClinicalReason();
                doctorName = userRepository.findById(order.getOrderedBy())
                        .map(User::getFullName)
                        .orElse(null);
            }
        }

        if (doctorName == null && visit.getDoctorId() != null) {
            doctorName = userRepository.findById(visit.getDoctorId())
                    .map(User::getFullName)
                    .orElse("-");
        }

        String specialtyName = specialtyRepository.findById(visit.getSpecialtyId())
                .map(Specialty::getName)
                .orElse("-");

        String dobStr = patient.getDateOfBirth() != null ? DATE_FORMATTER.format(patient.getDateOfBirth()) : "-";
        String genderStr = mapGender(patient.getGender() != null ? patient.getGender().name() : null);

        String resultValue = resolveResultValue(result);

        ClinicalResultPrintItem printItem = new ClinicalResultPrintItem(
                1,
                item != null ? item.getServiceCode() : "-",
                item != null ? item.getServiceName() : "Dịch vụ cận lâm sàng",
                result.getResultType() != null ? result.getResultType().name() : "NUMBER",
                resultValue,
                result.getUnit(),
                result.getReferenceRange(),
                result.getAbnormalFlag() != null ? result.getAbnormalFlag().name() : "NORMAL",
                result.getConclusion()
        );

        Instant now = clockPort.now();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        ClinicalResultPrintDocument doc = new ClinicalResultPrintDocument(
                clinicName,
                clinicAddress,
                clinicPhone,
                patient.getPatientCode(),
                patient.getFullName(),
                dobStr,
                genderStr,
                patient.getPhone(),
                visit.getVisitCode(),
                visit.getVisitAt(),
                doctorName,
                specialtyName,
                orderCode,
                clinicalReason,
                List.of(printItem),
                result.getConclusion(),
                now
        );

        byte[] pdfBytes = clinicalResultPdfRenderer.render(doc);

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.EXPORT,
                ResourceType.CLINICAL_RESULT,
                result.getId(),
                auditDetail(visit, List.of(result.getId()), now),
                null,
                now
        ));

        String serviceCode = item != null ? item.getServiceCode() : "CLS";
        String fileName = "ket-qua-" + serviceCode + "-" + visit.getVisitCode() + ".pdf";
        return new ClinicalResultPrintResult(fileName, PDF_CONTENT_TYPE, pdfBytes);
    }

    @Override
    public ClinicalResultPrintResult exportByVisit(UUID visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));

        patientAccessGuard.requirePatientOwnership(
                visit.getPatientId(),
                ResourceType.CLINICAL_RESULT,
                visitId
        );

        Patient patient = patientRepository.findById(visit.getPatientId())
                .orElseThrow(() -> new PatientNotFoundException(visit.getPatientId()));

        List<ClinicalResult> results = clinicalResultRepository.findByVisitIdAndStatus(visitId, ClinicalResultStatus.FINAL);
        if (results.isEmpty()) {
            throw new ClinicalResultNotFoundException("Không tìm thấy kết quả cận lâm sàng đã xác nhận cho lượt khám này.");
        }

        ClinicConfiguration clinic = clinicConfigurationRepository.find().orElse(null);
        String clinicName = clinic != null ? clinic.getClinicName() : "Phòng khám Đa khoa";
        String clinicAddress = clinic != null ? clinic.getAddress() : "-";
        String clinicPhone = clinic != null ? clinic.getPhone() : "-";

        String doctorName = userRepository.findById(visit.getDoctorId())
                .map(User::getFullName)
                .orElse("-");

        String specialtyName = specialtyRepository.findById(visit.getSpecialtyId())
                .map(Specialty::getName)
                .orElse("-");

        String dobStr = patient.getDateOfBirth() != null ? DATE_FORMATTER.format(patient.getDateOfBirth()) : "-";
        String genderStr = mapGender(patient.getGender() != null ? patient.getGender().name() : null);

        List<UUID> itemIds = results.stream().map(ClinicalResult::getClinicalOrderItemId).toList();
        Map<UUID, ClinicalOrderItem> itemMap = clinicalOrderItemRepository.findByIdIn(itemIds).stream()
                .collect(Collectors.toMap(ClinicalOrderItem::getId, Function.identity(), (a, b) -> a));

        List<ClinicalResultPrintItem> printItems = new ArrayList<>();
        StringBuilder overallConclusion = new StringBuilder();

        for (int i = 0; i < results.size(); i++) {
            ClinicalResult r = results.get(i);
            ClinicalOrderItem item = itemMap.get(r.getClinicalOrderItemId());
            String valStr = resolveResultValue(r);

            printItems.add(new ClinicalResultPrintItem(
                    i + 1,
                    item != null ? item.getServiceCode() : "-",
                    item != null ? item.getServiceName() : "Dịch vụ cận lâm sàng",
                    r.getResultType() != null ? r.getResultType().name() : "NUMBER",
                    valStr,
                    r.getUnit(),
                    r.getReferenceRange(),
                    r.getAbnormalFlag() != null ? r.getAbnormalFlag().name() : "NORMAL",
                    r.getConclusion()
            ));

            if (r.getConclusion() != null && !r.getConclusion().isBlank()) {
                if (!overallConclusion.isEmpty()) {
                    overallConclusion.append("; ");
                }
                overallConclusion.append(r.getConclusion());
            }
        }

        Instant now = clockPort.now();
        UUID currentUserId = currentUserPort.getCurrentUserId();

        ClinicalResultPrintDocument doc = new ClinicalResultPrintDocument(
                clinicName,
                clinicAddress,
                clinicPhone,
                patient.getPatientCode(),
                patient.getFullName(),
                dobStr,
                genderStr,
                patient.getPhone(),
                visit.getVisitCode(),
                visit.getVisitAt(),
                doctorName,
                specialtyName,
                null,
                null,
                printItems,
                overallConclusion.toString(),
                now
        );

        byte[] pdfBytes = clinicalResultPdfRenderer.render(doc);

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.EXPORT,
                ResourceType.CLINICAL_RESULT,
                visit.getId(),
                auditDetail(visit, results.stream().map(ClinicalResult::getId).toList(), now),
                null,
                now
        ));

        String fileName = "ket-qua-can-lam-sang-" + visit.getVisitCode() + ".pdf";
        return new ClinicalResultPrintResult(fileName, PDF_CONTENT_TYPE, pdfBytes);
    }

    private String resolveResultValue(ClinicalResult result) {
        if (result.getResultType() == ClinicalResultType.NUMBER && result.getNumericValue() != null) {
            return result.getNumericValue().stripTrailingZeros().toPlainString();
        } else if (result.getTextValue() != null && !result.getTextValue().isBlank()) {
            return result.getTextValue();
        } else if (result.getConclusion() != null) {
            return result.getConclusion();
        }
        return "-";
    }

    private String mapGender(String gender) {
        if (gender == null) {
            return "-";
        }
        return switch (gender.toUpperCase()) {
            case "MALE" -> "Nam";
            case "FEMALE" -> "Nữ";
            default -> "Khác";
        };
    }

    private String auditDetail(Visit visit, List<UUID> resultIds, Instant exportedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("channel", ONLINE_PORTAL);
        detail.put("visitId", visit.getId().toString());
        detail.put("patientId", visit.getPatientId().toString());
        detail.put("exportedResultIds", resultIds.stream().map(UUID::toString).toList());
        detail.put("exportedAt", exportedAt.toString());

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize clinical result export audit detail.", exception);
        }
    }
}
