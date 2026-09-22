package com.benhsoan.application.ucservice.billing;

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
import lombok.extern.slf4j.Slf4j;

/**
 * Writes payment access-denied audit entries in an independent transaction (REQUIRES_NEW)
 * so the security trace survives business transaction rollbacks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingAccessDeniedAuditWriter {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writePaymentDenied(
            UUID actorId,
            UUID visitId,
            Instant deniedAt,
            String errorReason
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("error", errorReason != null ? errorReason : "User lacks RECEPTIONIST or ADMIN role to record payments");
        detail.put("visitId", visitId != null ? visitId.toString() : null);
        detail.put("deniedAt", deniedAt != null ? deniedAt.toString() : null);

        try {
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.ACCESS_DENIED,
                    ResourceType.PAYMENT,
                    visitId,
                    toJson(detail),
                    null,
                    deniedAt
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to record payment access denied audit log for actor {} on visit {}: {}",
                    actorId, visitId, exception.getMessage());
        }
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize audit detail payload to JSON", ex);
            return "{}";
        }
    }
}
