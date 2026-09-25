package com.benhsoan.persistence.mapper.backup;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.backup.BackupScheduleConfiguration;
import com.benhsoan.persistence.entity.backup.BackupScheduleConfigurationEntity;

@Component
public class BackupSchedulePersistenceMapper {

    public BackupScheduleConfiguration toDomain(BackupScheduleConfigurationEntity entity) {
        if (entity == null) {
            return null;
        }
        return BackupScheduleConfiguration.restore(
                entity.getId(),
                entity.isEnabled(),
                entity.getDailyTime(),
                entity.getCronExpression(),
                entity.getLastRunAt(),
                entity.getLastStatus(),
                entity.getLastFailureReason(),
                entity.isAlertActive(),
                entity.getLastVerifiedAt(),
                entity.getLastVerificationStatus(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }

    public BackupScheduleConfigurationEntity toEntity(BackupScheduleConfiguration domain) {
        if (domain == null) {
            return null;
        }
        return BackupScheduleConfigurationEntity.builder()
                .id(domain.getId())
                .enabled(domain.isEnabled())
                .dailyTime(domain.getDailyTime())
                .cronExpression(domain.getCronExpression())
                .lastRunAt(domain.getLastRunAt())
                .lastStatus(domain.getLastStatus())
                .lastFailureReason(domain.getLastFailureReason())
                .alertActive(domain.isAlertActive())
                .lastVerifiedAt(domain.getLastVerifiedAt())
                .lastVerificationStatus(domain.getLastVerificationStatus())
                .updatedBy(domain.getUpdatedBy())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
