package com.benhsoan.persistence.jpaRepository.backup;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.benhsoan.persistence.entity.backup.BackupScheduleConfigurationEntity;

@Repository
public interface JpaBackupScheduleConfigurationRepository
        extends JpaRepository<BackupScheduleConfigurationEntity, UUID> {

    Optional<BackupScheduleConfigurationEntity> findFirstByOrderByUpdatedAtDesc();
}
