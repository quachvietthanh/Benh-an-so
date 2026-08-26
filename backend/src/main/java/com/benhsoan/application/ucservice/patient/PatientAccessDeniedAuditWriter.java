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
 * Writes patient data-scope denial audit entries (NCL-14-CN-002 TC-03 / QTN-23) in an
 * independent transaction so the refusal record survives the rollback triggered by the
 * subsequent {@code AccessDeniedException}.
 */
@Component
@RequiredArgsConstructor
public class PatientAccessDeniedAuditWriter {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeDenied(UUID actorId, UUID targetPatientId, Instant deniedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", "ACCESS_DENIED");
        detail.put("targetPatientId", targetPatientId.toString());
        detail.put("deniedAt", deniedAt.toString());

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.ACCESS_DENIED,
                ResourceType.PATIENT,
                targetPatientId,
                toJson(detail),
                null,
                deniedAt
        ));
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize patient access denial audit detail.", exception);
        }
    }
}
