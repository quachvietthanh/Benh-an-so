package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeErasureRefusal(
            UUID actorId,
            UUID patientId,
            String patientCode,
            int retentionYears,
            String reason,
            Instant now
    ) {
        auditLogRepository.save(
                AuditLog.create(
                        actorId,
                        ActionType.UPDATE,
                        ResourceType.PATIENT,
                        patientId,
                        """
                        {
                        "action":"DATA_ERASURE_REQUEST",
                        "patientCode":"%s",
                        "medicalRecordsRetained":true,
                        "retentionYears":%d,
                        "consentWithdrawn":true,
                        "nonMedicalUseRestricted":true,
                        "reason":"%s",
                        "note":"Từ chối xóa hồ sơ bệnh án theo Luật Khám bệnh, chữa bệnh và QTN-19; đã rút lại sự đồng ý ngoài khám chữa bệnh."
                        }
                        """.formatted(
                                patientCode != null ? patientCode : "",
                                retentionYears,
                                reason != null ? reason : ""
                        ),
                        null,
                        now
                )
        );
    }
}
