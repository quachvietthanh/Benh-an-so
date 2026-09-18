package com.benhsoan.application.ucservice.auditlog;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.RequiredArgsConstructor;

/**
 * Writes QTN-31 administrative operation audit records into the existing
 * {@link AuditLog} infrastructure. Each record captures the previous value
 * (before) and the new value (after) together with the actor and timestamp.
 *
 * <p>Administrative logs are append-only: this service only ever {@code save}s
 * new records; it never updates or deletes them.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminOperationAuditService {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private final AuditLogRepository auditLogRepository;

    /**
     * Records an immutable administrative operation audit entry.
     *
     * @param actorId      authenticated actor (never from the request body)
     * @param action       the action type
     * @param resourceType the resource/object type
     * @param resourceId   the target object id (may be {@code null})
     * @param before       previous value snapshot (may be {@code null} for CREATE)
     * @param after        new value snapshot
     * @param at           the operation timestamp
     */
    public void record(UUID actorId, ActionType action, ResourceType resourceType, UUID resourceId,
            Map<String, Object> before, Map<String, Object> after, Instant at) {
        auditLogRepository.save(AuditLog.create(
                actorId,
                action,
                resourceType,
                resourceId,
                toDetail(before, after),
                null,
                at));
    }

    /**
     * Ordered, null-tolerant snapshot builder. Pass key/value pairs in order:
     * {@code fields("price", oldPrice, "effectiveFrom", date)}.
     */
    public static Map<String, Object> fields(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private String toDetail(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", after);
        try {
            return OBJECT_MAPPER.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize administrative operation audit detail.", exception);
        }
    }
}
