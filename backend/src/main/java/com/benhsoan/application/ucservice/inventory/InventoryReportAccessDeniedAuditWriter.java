package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Writes inventory report access-denied audit entries (NCL-06-CN-013 TC-02)
 * in an independent transaction (REQUIRES_NEW) so the refusal record survives
 * any rollback of the calling business transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReportAccessDeniedAuditWriter {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeAccessDenied(
            UUID actorId,
            Set<String> roles,
            String reason,
            Instant deniedAt
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("resource", "INVENTORY_IN_OUT_STOCK_REPORT");
        detail.put("roles", roles != null && !roles.isEmpty() ? String.join(",", roles) : "NONE");
        detail.put("reason", reason != null ? reason : "Access denied to inventory report");
        detail.put("deniedAt", deniedAt != null ? deniedAt.toString() : Instant.now().toString());

        try {
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.ACCESS_DENIED,
                    ResourceType.OPERATIONAL_REPORT,
                    null,
                    toJson(detail),
                    null,
                    deniedAt
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to record access denied audit log for inventory report: {}", exception.getMessage());
        }
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize audit detail to JSON: {}", exception.getMessage());
            return "{\"resource\":\"INVENTORY_IN_OUT_STOCK_REPORT\",\"error\":\"serialization_failed\"}";
        }
    }
}
