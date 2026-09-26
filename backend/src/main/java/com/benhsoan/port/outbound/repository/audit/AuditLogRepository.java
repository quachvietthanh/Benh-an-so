package com.benhsoan.port.outbound.repository.audit;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ResourceType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Read/write port for {@link AuditLog}. The administrative operation log
 * (NCL-09-CN-006) is intentionally a read-only projection over the same store:
 * this port exposes only {@code save} (append) and query operations. There is
 * deliberately no update or delete method, so historical records are immutable
 * from the application's perspective (TC-04).
 */
public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(UUID id);

    Page<AuditLog> findLoginAuditLogs(UUID userId, Pageable pageable);

    java.util.List<AuditLog> findByResourceTypeAndResourceId(
            ResourceType resourceType,
            UUID resourceId);

    Page<AuditLog> findAdminOperationLogs(
            UUID actorId,
            ResourceType resourceType,
            Instant from,
            Instant to,
            Pageable pageable);
}
