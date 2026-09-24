package com.benhsoan.persistence.mapper.backup;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.backup.BackupSchedule;
import com.benhsoan.persistence.entity.backup.BackupScheduleEntity;

@Component
public class BackupSchedulePersistenceMapper {

    public BackupSchedule toDomain(BackupScheduleEntity entity) {
        if (entity == null) {
            return null;
        }
        return BackupSchedule.restore(
                entity.getId(),
                entity.isEnabled(),
                entity.getBackupTime(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public BackupScheduleEntity toEntity(BackupSchedule domain) {
        if (domain == null) {
            return null;
        }
        return BackupScheduleEntity.builder()
                .id(domain.getId())
                .enabled(domain.isEnabled())
                .backupTime(domain.getBackupTime())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
