package com.benhsoan.persistence.adapterRepository.backup;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.persistence.jpaRepository.backup.JpaBackupRecordRepository;
import com.benhsoan.persistence.mapper.backup.BackupPersistenceMapper;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BackupRecordRepositoryAdapter implements BackupRecordRepository {

    private final JpaBackupRecordRepository jpaRepository;
    private final BackupPersistenceMapper mapper;

    @Override
    @Transactional
    public BackupRecord save(BackupRecord record) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(record)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BackupRecord> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BackupRecord> findAllByOrderByCreatedAtDesc() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BackupRecord> findTopByOrderByBackupCodeDesc() {
        return jpaRepository.findTopByOrderByBackupCodeDesc().map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveInProgressBackup(Instant createdAfter) {
        return jpaRepository.existsByStatusAndCreatedAtAfter(BackupStatus.IN_PROGRESS, createdAfter);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BackupRecord> findLatestByStatus(BackupStatus status) {
        return jpaRepository.findFirstByStatusOrderByCreatedAtDesc(status).map(mapper::toDomain);
    }
}
