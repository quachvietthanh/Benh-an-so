package com.benhsoan.application.ucservice.inventory;

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
 * Ghi nhận nhật ký kiểm toán độc lập cho quy trình dự trù mua thuốc (NCL-06-CN-012).
 * Các thao tác bị từ chối do vi phạm phân quyền SoD được ghi trong transaction độc lập (REQUIRES_NEW)
 * để bảo đảm nhật ký không bị xóa khi transaction nghiệp vụ bị rollback.
 */
@Component
@RequiredArgsConstructor
public class MedicationProcurementAuditWriter {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeSoDDenied(UUID actorId, UUID planId, String planCode, Instant now) {
        writeApproveSoDDenied(actorId, planId, planCode, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeApproveSoDDenied(UUID actorId, UUID planId, String planCode, Instant now) {
        writeSoDDeniedInternal(actorId, planId, planCode, "APPROVE_DENIED_SOD",
                "Người lập phiếu không được phép tự duyệt phiếu của chính mình", now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeRejectSoDDenied(UUID actorId, UUID planId, String planCode, Instant now) {
        writeSoDDeniedInternal(actorId, planId, planCode, "REJECT_DENIED_SOD",
                "Người lập phiếu không được phép tự từ chối phiếu của chính mình", now);
    }

    private void writeSoDDeniedInternal(UUID actorId, UUID planId, String planCode, String action, String reason, Instant now) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("planId", planId != null ? planId.toString() : null);
        payload.put("planCode", planCode);
        payload.put("reason", reason);
        payload.put("deniedAt", now != null ? now.toString() : null);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            json = "{}";
        }

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.ACCESS_DENIED,
                ResourceType.MEDICATION_PROCUREMENT_PLAN,
                planId,
                json,
                null,
                now
        ));
    }
}
