package com.benhsoan.persistence.jpaRepository.auditlog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.auditlog.AuditLogEntity;

public interface JpaAuditLogRepository
        extends JpaRepository<AuditLogEntity, UUID> {
}
