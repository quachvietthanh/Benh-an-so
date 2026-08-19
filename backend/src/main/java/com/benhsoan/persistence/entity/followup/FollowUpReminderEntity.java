package com.benhsoan.persistence.entity.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;

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
@Table(name = "follow_up_reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpReminderEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "visit_id", columnDefinition = "BINARY(16)")
    private UUID visitId;

    @Column(name = "appointment_id", columnDefinition = "BINARY(16)")
    private UUID appointmentId;

    @Column(name = "follow_up_date", nullable = false)
    private LocalDate followUpDate;

    @Column(name = "remind_at", nullable = false)
    private Instant remindAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 30)
    private ReminderType reminderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReminderStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
