package com.benhsoan.persistence.entity.backup;

import java.time.Instant;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "backup_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupScheduleEntity {

    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "backup_time", nullable = false)
    private LocalTime backupTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
