package com.benhsoan.port.outbound.repository.audit;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.auditlog.AuditLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(UUID id);

    Page<AuditLog> findLoginAuditLogs(UUID userId, Pageable pageable);
}
