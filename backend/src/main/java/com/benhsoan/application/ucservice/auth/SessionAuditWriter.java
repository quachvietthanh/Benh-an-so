package com.benhsoan.application.ucservice.auth;

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
 * Writes session audit entries (NCL-01-CN-007 / QTN-45, QTN-01)
 * in an independent transaction so the record survives even if outer transaction rolls back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAuditWriter {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeTerminationAudit(
            UUID actorId,
            UUID sessionId,
            UUID targetUserId,
            String reason,
            Instant terminatedAt
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("sessionId", sessionId != null ? sessionId.toString() : null);
        detail.put("targetUserId", targetUserId != null ? targetUserId.toString() : null);
        detail.put("reason", reason != null ? reason : "Session terminated by administrator");
        detail.put("terminatedAt", terminatedAt != null ? terminatedAt.toString() : null);

        try {
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.DEACTIVATE,
                    ResourceType.USER_SESSION,
                    sessionId,
                    toJson(detail),
                    null,
                    terminatedAt
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to record session termination audit log for actor {} on session {}: {}",
                    actorId, sessionId, exception.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeAccessDeniedAudit(
            UUID actorId,
            String action,
            Instant deniedAt,
            String reason
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", action);
        detail.put("reason", reason);
        detail.put("deniedAt", deniedAt != null ? deniedAt.toString() : null);

        try {
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.ACCESS_DENIED,
                    ResourceType.USER_SESSION,
                    actorId,
                    toJson(detail),
                    null,
                    deniedAt
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to record session access denied audit log for actor {}: {}",
                    actorId, exception.getMessage());
        }
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize session audit detail.", exception);
        }
    }
}
