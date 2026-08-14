package com.benhsoan.persistence.jpaRepository.backup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.backup.BackupRecordEntity;

public interface JpaBackupRecordRepository
        extends JpaRepository<BackupRecordEntity, UUID> {

    List<BackupRecordEntity> findAllByOrderByCreatedAtDesc();

    Optional<BackupRecordEntity> findTopByOrderByBackupCodeDesc();
}
