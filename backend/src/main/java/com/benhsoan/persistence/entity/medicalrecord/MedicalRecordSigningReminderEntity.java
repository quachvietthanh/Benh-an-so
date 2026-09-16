package com.benhsoan.persistence.entity.medicalrecord;

import java.time.Instant;
import java.util.UUID;

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
@Table(name = "medical_record_signing_reminders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordSigningReminderEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "medical_record_id", nullable = false)
    private UUID medicalRecordId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "reminded_by", nullable = false)
    private UUID remindedBy;

    @Column(name = "reminded_at", nullable = false)
    private Instant remindedAt;

    @Column(name = "overdue_hours", nullable = false)
    private long overdueHours;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", nullable = false, length = 30)
    private String status;
}
