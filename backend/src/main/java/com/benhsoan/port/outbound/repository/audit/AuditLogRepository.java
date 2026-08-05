package com.benhsoan.port.outbound.repository.audit;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.auditlog.AuditLog;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(UUID id);
}
