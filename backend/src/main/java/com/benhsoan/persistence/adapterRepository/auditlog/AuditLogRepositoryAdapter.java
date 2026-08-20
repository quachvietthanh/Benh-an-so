package com.benhsoan.persistence.adapterRepository.auditlog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.persistence.entity.auditlog.AuditLogEntity;
import com.benhsoan.persistence.jpaRepository.auditlog.JpaAuditLogRepository;
import com.benhsoan.persistence.mapper.auditlog.AuditLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter
        implements AuditLogRepository {

    private final JpaAuditLogRepository jpaRepository;
    private final AuditLogPersistenceMapper mapper;

    @Override
    public AuditLog save(AuditLog auditLog) {

        AuditLogEntity entity = mapper.toEntity(auditLog);
        return mapper.toDomain(
                jpaRepository.save(entity)
        );
    }

    @Override
    public Optional<AuditLog> findById(UUID id) {
        if(id == null) return null;
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

}
