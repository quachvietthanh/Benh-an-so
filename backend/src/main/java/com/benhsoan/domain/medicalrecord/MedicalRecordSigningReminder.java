package com.benhsoan.domain.medicalrecord;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalRecordSigningReminder {

    private UUID id;
    private UUID medicalRecordId;
    private UUID doctorId;
    private UUID remindedBy;
    private Instant remindedAt;
    private long overdueHours;
    private String channel;
    private String notes;
    private String status;

    private MedicalRecordSigningReminder(
            UUID id,
            UUID medicalRecordId,
            UUID doctorId,
            UUID remindedBy,
            Instant remindedAt,
            long overdueHours,
            String channel,
            String notes,
            String status
    ) {
        this.id = Objects.requireNonNull(id, "Reminder id is required.");
        this.medicalRecordId = Objects.requireNonNull(medicalRecordId, "Medical record id is required.");
        this.doctorId = Objects.requireNonNull(doctorId, "Doctor id is required.");
        this.remindedBy = Objects.requireNonNull(remindedBy, "Reminded by user id is required.");
        this.remindedAt = Objects.requireNonNull(remindedAt, "Reminded at timestamp is required.");
        if (overdueHours < 0) {
            throw new ValidationException("Overdue hours must be non-negative.");
        }
        this.overdueHours = overdueHours;
        this.channel = (channel != null && !channel.isBlank()) ? channel.trim() : "MOCK";
        this.notes = notes != null ? notes.trim() : null;
        this.status = (status != null && !status.isBlank()) ? status.trim() : "SENT";
    }

    public static MedicalRecordSigningReminder create(
            UUID medicalRecordId,
            UUID doctorId,
            UUID remindedBy,
            Instant remindedAt,
            long overdueHours,
            String channel,
            String notes
    ) {
        return create(
                medicalRecordId,
                doctorId,
                remindedBy,
                remindedAt,
                overdueHours,
                channel,
                notes,
                "SENT"
        );
    }

    public static MedicalRecordSigningReminder create(
            UUID medicalRecordId,
            UUID doctorId,
            UUID remindedBy,
            Instant remindedAt,
            long overdueHours,
            String channel,
            String notes,
            String status
    ) {
        return new MedicalRecordSigningReminder(
                UUID.randomUUID(),
                medicalRecordId,
                doctorId,
                remindedBy,
                remindedAt,
                overdueHours,
                channel,
                notes,
                status
        );
    }

    public static MedicalRecordSigningReminder restore(
            UUID id,
            UUID medicalRecordId,
            UUID doctorId,
            UUID remindedBy,
            Instant remindedAt,
            long overdueHours,
            String channel,
            String notes,
            String status
    ) {
        return new MedicalRecordSigningReminder(
                id,
                medicalRecordId,
                doctorId,
                remindedBy,
                remindedAt,
                overdueHours,
                channel,
                notes,
                status
        );
    }
}
