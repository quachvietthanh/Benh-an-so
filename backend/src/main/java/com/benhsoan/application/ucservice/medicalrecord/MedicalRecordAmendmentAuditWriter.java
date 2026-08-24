package com.benhsoan.application.ucservice.medicalrecord;

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
 * Writes medical record amendment audit entries (NCL-11-CN-002). The denial entry
 * is written in an independent transaction so it survives the rollback triggered by
 * the authorization failure (TC-03).
 */
@Component
@RequiredArgsConstructor
public class MedicalRecordAmendmentAuditWriter {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeDenied(UUID actorId, UUID medicalRecordId, String reason, Instant now) {
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.ACCESS_DENIED,
                ResourceType.MEDICAL_RECORD,
                medicalRecordId,
                toJson(Map.of(
                        "action", "ACCESS_DENIED",
                        "reason", reason,
                        "deniedAt", now.toString()
                )),
                null,
                now
        ));
    }

    @Transactional
    public void writeAmended(UUID actorId, UUID medicalRecordId, String reason, Instant amendedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", "AMEND");
        detail.put("amendedBy", actorId.toString());
        detail.put("reason", reason);
        detail.put("amendedAt", amendedAt.toString());
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.UPDATE,
                ResourceType.MEDICAL_RECORD,
                medicalRecordId,
                toJson(detail),
                null,
                amendedAt
        ));
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize medical record amendment audit detail.", exception);
        }
    }
}
