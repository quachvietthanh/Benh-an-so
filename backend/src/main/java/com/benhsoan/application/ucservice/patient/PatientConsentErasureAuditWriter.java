package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Ghi nhận nhật ký kiểm toán cho yêu cầu xóa dữ liệu của người bệnh.
 * Nhật ký từ chối xóa hồ sơ bệnh án được ghi trong giao dịch độc lập (REQUIRES_NEW)
 * để đảm bảo không bị mất kể cả khi giao dịch nghiệp vụ xảy ra rollback (QTN-19).
 */
@Component
@RequiredArgsConstructor
public class PatientConsentErasureAuditWriter {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeErasureRefusal(
            UUID actorId,
            UUID patientId,
            String patientCode,
            int retentionYears,
            String reason,
            Instant now
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", "DATA_ERASURE_REQUEST");
        detail.put("patientCode", patientCode != null ? patientCode : "");
        detail.put("medicalRecordsRetained", true);
        detail.put("retentionYears", retentionYears);
        detail.put("consentWithdrawn", true);
        detail.put("nonMedicalUseRestricted", true);
        detail.put("reason", reason != null ? reason : "");
        detail.put("note", "Từ chối xóa hồ sơ bệnh án theo Luật Khám bệnh, chữa bệnh và QTN-19; đã rút lại sự đồng ý ngoài khám chữa bệnh.");

        auditLogRepository.save(
                AuditLog.create(
                        actorId,
                        ActionType.UPDATE,
                        ResourceType.PATIENT,
                        patientId,
                        toJson(detail),
                        null,
                        now
                )
        );
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể serialize chi tiết kiểm toán xóa dữ liệu người bệnh.", exception);
        }
    }
}
