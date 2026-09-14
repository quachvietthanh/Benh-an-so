package com.benhsoan.application.ucservice.appointment;

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
 * Writes appointment reschedule access-denied audit entries (NCL-03-CN-007 / QTN-01)
 * in an independent transaction so the refusal record survives the rollback
 * triggered by the subsequent UnauthorizedAppointmentOperationException.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentAccessDeniedAuditWriter {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeRescheduleDenied(
            UUID actorId,
            UUID appointmentId,
            Instant deniedAt,
            String errorReason
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("error", errorReason != null ? errorReason : "User lacks RECEPTIONIST or ADMIN role to reschedule appointment");
        detail.put("deniedAt", deniedAt != null ? deniedAt.toString() : null);

        try {
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.ACCESS_DENIED,
                    ResourceType.APPOINTMENT,
                    appointmentId,
                    toJson(detail),
                    null,
                    deniedAt
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to record access denied audit log for actor {} on appointment {}: {}",
                    actorId, appointmentId, exception.getMessage());
        }
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize appointment reschedule access denial audit detail.", exception);
        }
    }
}
