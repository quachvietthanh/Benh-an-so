package com.benhsoan.application.ucservice.billing;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to write cashier shift access-denied audit entries in an independent
 * transaction (REQUIRES_NEW) so refusal records survive subsequent business
 * transaction rollbacks (Finding 4, NCL-07-CN-009).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashierShiftAuthorizationAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordConfirmAccessDenied(UUID actorId, UUID shiftId, String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", "CONFIRM_SHIFT");
        details.put("shiftId", shiftId != null ? shiftId.toString() : null);
        details.put("reason", reason);
        details.put("deniedAt", Instant.now().toString());

        saveAuditLog(actorId, ResourceType.CASHIER_SHIFT, shiftId, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCloseAccessDenied(UUID actorId, String reason) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", "CLOSE_SHIFT");
        details.put("reason", reason);
        details.put("deniedAt", Instant.now().toString());

        saveAuditLog(actorId, ResourceType.CASHIER_SHIFT, null, details);
    }

    private void saveAuditLog(
            UUID actorId,
            ResourceType resourceType,
            UUID resourceId,
            Map<String, Object> details
    ) {
        try {
            String detailJson = objectMapper.writeValueAsString(details);
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.ACCESS_DENIED,
                    resourceType,
                    resourceId,
                    detailJson,
                    null,
                    Instant.now()
            ));
        } catch (Exception exception) {
            log.warn("Failed to record access denied audit log for actor {} on {} {}: {}",
                    actorId, resourceType, resourceId, exception.getMessage());
        }
    }
}
