package com.benhsoan.persistence.entity.backup;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.backup.enums.BackupStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "backup_schedule_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupScheduleConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "daily_time", nullable = false, length = 10)
    private String dailyTime;

    @Column(name = "cron_expression", nullable = false, length = 50)
    private String cronExpression;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_status", length = 30)
    private BackupStatus lastStatus;

    @Column(name = "last_failure_reason", columnDefinition = "TEXT")
    private String lastFailureReason;

    @Column(name = "alert_active", nullable = false)
    private boolean alertActive;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "last_verification_status", length = 30)
    private String lastVerificationStatus;

    @Column(name = "updated_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
