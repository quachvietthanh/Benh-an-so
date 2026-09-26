package com.benhsoan.persistence.jpaRepository.backup;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.persistence.entity.backup.BackupRecordEntity;

public interface JpaBackupRecordRepository
        extends JpaRepository<BackupRecordEntity, UUID> {

    List<BackupRecordEntity> findAllByOrderByCreatedAtDesc();

    Optional<BackupRecordEntity> findTopByOrderByBackupCodeDesc();

    boolean existsByStatusAndCreatedAtAfter(BackupStatus status, Instant createdAt);

    Optional<BackupRecordEntity> findFirstByStatusOrderByCreatedAtDesc(BackupStatus status);
}
