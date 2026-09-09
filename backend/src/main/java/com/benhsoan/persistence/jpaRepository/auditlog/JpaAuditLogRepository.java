package com.benhsoan.persistence.jpaRepository.auditlog;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.auditlog.AuditLogEntity;

public interface JpaAuditLogRepository
        extends JpaRepository<AuditLogEntity, UUID> {

    @Query("""
                SELECT a FROM AuditLogEntity a
                WHERE (a.userId = :userId AND a.actionType IN (
                    com.benhsoan.domain.auditlog.enums.ActionType.LOGIN,
                    com.benhsoan.domain.auditlog.enums.ActionType.LOGOUT,
                    com.benhsoan.domain.auditlog.enums.ActionType.LOGIN_FAILED,
                    com.benhsoan.domain.auditlog.enums.ActionType.LOCK
                ))
                OR (a.resourceType = com.benhsoan.domain.auditlog.enums.ResourceType.USER
                    AND a.resourceId = :userId
                    AND a.actionType IN (
                    com.benhsoan.domain.auditlog.enums.ActionType.LOCK,
                    com.benhsoan.domain.auditlog.enums.ActionType.UNLOCK
                ))
                ORDER BY a.createdAt DESC
            """)
    Page<AuditLogEntity> findLoginAuditLogs(
            @Param("userId") UUID userId,
            Pageable pageable);
}
