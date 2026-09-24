package com.benhsoan.persistence.jpaRepository.backup;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.backup.BackupScheduleEntity;

public interface JpaBackupScheduleRepository extends JpaRepository<BackupScheduleEntity, Integer> {
}
