package com.benhsoan.persistence.jpaRepository.auditlog;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.auditlog.enums.ResourceType;
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

    java.util.List<AuditLogEntity> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            com.benhsoan.domain.auditlog.enums.ResourceType resourceType,
            UUID resourceId);

    @Query("""
                SELECT a FROM AuditLogEntity a
                WHERE (
                    (a.resourceType = com.benhsoan.domain.auditlog.enums.ResourceType.USER
                        AND a.actionType IN (
                            com.benhsoan.domain.auditlog.enums.ActionType.CREATE,
                            com.benhsoan.domain.auditlog.enums.ActionType.UPDATE,
                            com.benhsoan.domain.auditlog.enums.ActionType.ACTIVATE,
                            com.benhsoan.domain.auditlog.enums.ActionType.DEACTIVATE,
                            com.benhsoan.domain.auditlog.enums.ActionType.UNLOCK
                        ))
                    OR (a.resourceType = com.benhsoan.domain.auditlog.enums.ResourceType.ROLE
                        AND a.actionType = com.benhsoan.domain.auditlog.enums.ActionType.UPDATE)
                    OR (a.resourceType = com.benhsoan.domain.auditlog.enums.ResourceType.MEDICINE
                        AND a.actionType IN (
                            com.benhsoan.domain.auditlog.enums.ActionType.CREATE,
                            com.benhsoan.domain.auditlog.enums.ActionType.UPDATE,
                            com.benhsoan.domain.auditlog.enums.ActionType.ACTIVATE,
                            com.benhsoan.domain.auditlog.enums.ActionType.DEACTIVATE
                        ))
                    OR (a.resourceType = com.benhsoan.domain.auditlog.enums.ResourceType.SERVICE_CATALOG
                        AND a.actionType IN (
                            com.benhsoan.domain.auditlog.enums.ActionType.CREATE,
                            com.benhsoan.domain.auditlog.enums.ActionType.UPDATE,
                            com.benhsoan.domain.auditlog.enums.ActionType.ACTIVATE,
                            com.benhsoan.domain.auditlog.enums.ActionType.DEACTIVATE
                        ))
                    OR (a.resourceType = com.benhsoan.domain.auditlog.enums.ResourceType.SERVICE_PRICE
                        AND a.actionType = com.benhsoan.domain.auditlog.enums.ActionType.CREATE)
                )
                AND (:actorId IS NULL OR a.userId = :actorId)
                AND (:resourceType IS NULL OR a.resourceType = :resourceType)
                AND (:from IS NULL OR a.createdAt >= :from)
                AND (:to IS NULL OR a.createdAt < :to)
                ORDER BY a.createdAt DESC
            """)
    Page<AuditLogEntity> findAdminOperationLogs(
            @Param("actorId") UUID actorId,
            @Param("resourceType") ResourceType resourceType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
