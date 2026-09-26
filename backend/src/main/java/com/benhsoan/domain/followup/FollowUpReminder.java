package com.benhsoan.domain.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.domain.followup.exception.FollowUpReminderInvalidStatusException;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FollowUpReminder {

    private UUID id;
    private UUID patientId;
    private UUID visitId;
    private UUID appointmentId;
    private LocalDate followUpDate;
    private Instant remindAt;
    private ReminderType reminderType;
    private ReminderStatus status;
    private String notes;
    private UUID createdBy;
    private Instant createdAt;

    private FollowUpReminder(
            UUID id,
            UUID patientId,
            UUID visitId,
            UUID appointmentId,
            LocalDate followUpDate,
            Instant remindAt,
            ReminderType reminderType,
            ReminderStatus status,
            String notes,
            UUID createdBy,
            Instant createdAt
    ) {
        this.id = Guard.require(id, "Reminder id");
        this.patientId = Guard.require(patientId, "Patient id");
        this.followUpDate = Guard.require(followUpDate, "Follow-up date");
        this.remindAt = Guard.require(remindAt, "Remind at");
        this.reminderType = Guard.require(reminderType, "Reminder type");
        this.status = Guard.require(status, "Status");
        this.createdBy = Guard.require(createdBy, "Created by");
        this.createdAt = Guard.require(createdAt, "Created at");

        this.visitId = visitId;
        this.appointmentId = appointmentId;
        this.notes = notes;
    }

    public static FollowUpReminder create(
            UUID patientId,
            UUID visitId,
            UUID appointmentId,
            LocalDate followUpDate,
            Instant remindAt,
            ReminderType reminderType,
            String notes,
            UUID createdBy,
            Instant createdAt
    ) {
        return new FollowUpReminder(
                UUID.randomUUID(),
                patientId,
                visitId,
                appointmentId,
                followUpDate,
                remindAt,
                reminderType,
                ReminderStatus.PENDING,
                notes,
                createdBy,
                createdAt
        );
    }

    public static FollowUpReminder restore(
            UUID id,
            UUID patientId,
            UUID visitId,
            UUID appointmentId,
            LocalDate followUpDate,
            Instant remindAt,
            ReminderType reminderType,
            ReminderStatus status,
            String notes,
            UUID createdBy,
            Instant createdAt
    ) {
        return new FollowUpReminder(
                id,
                patientId,
                visitId,
                appointmentId,
                followUpDate,
                remindAt,
                reminderType,
                status,
                notes,
                createdBy,
                createdAt
        );
    }

    public void updateStatus(ReminderStatus newStatus) {
        Guard.require(newStatus, "Status");

        if (newStatus == ReminderStatus.PENDING) {
            throw new ValidationException("Status cannot be set back to PENDING.");
        }
        if (status == ReminderStatus.COMPLETED || status == ReminderStatus.CANCELLED) {
            throw new FollowUpReminderInvalidStatusException(
                    "Cannot update a reminder that is already " + status + "."
            );
        }

        this.status = newStatus;
    }

    public boolean isPending() {
        return status == ReminderStatus.PENDING;
    }
}
