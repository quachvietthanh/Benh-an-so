package com.benhsoan.domain.appointment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A registered time-off / leave interval for a doctor (NCL-03-CN-006 / QTN-30).
 * Prevents appointments from being booked or rescheduled during this window.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DoctorTimeOff {

    private static final int MAX_REASON_LENGTH = 255;

    private UUID id;

    private UUID doctorId;

    private Instant startTime;

    private Instant endTime;

    private String reason;

    private TimeOffStatus status;

    private UUID createdBy;

    private Instant createdAt;

    private Instant updatedAt;

    private DoctorTimeOff(
            UUID id,
            UUID doctorId,
            Instant startTime,
            Instant endTime,
            String reason,
            TimeOffStatus status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Id cannot be null.");
        this.doctorId = Guard.require(doctorId, "Doctor id");
        this.startTime = Guard.require(startTime, "Start time");
        this.endTime = Guard.require(endTime, "End time");

        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("Time-off end time must be after start time.");
        }

        if (reason != null && reason.trim().length() > MAX_REASON_LENGTH) {
            throw new ValidationException("Time-off reason must not exceed " + MAX_REASON_LENGTH + " characters.");
        }

        this.reason = reason != null && !reason.isBlank() ? reason.trim() : null;
        this.status = status != null ? status : TimeOffStatus.ACTIVE;
        this.createdBy = Guard.require(createdBy, "Created by");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null.");
        this.updatedAt = updatedAt;
    }

    public static DoctorTimeOff create(
            UUID doctorId,
            Instant startTime,
            Instant endTime,
            String reason,
            UUID createdBy,
            Instant now
    ) {
        return new DoctorTimeOff(
                UUID.randomUUID(),
                doctorId,
                startTime,
                endTime,
                reason,
                TimeOffStatus.ACTIVE,
                createdBy,
                now != null ? now : Instant.now(),
                null
        );
    }

    public static DoctorTimeOff restore(
            UUID id,
            UUID doctorId,
            Instant startTime,
            Instant endTime,
            String reason,
            TimeOffStatus status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new DoctorTimeOff(
                id,
                doctorId,
                startTime,
                endTime,
                reason,
                status,
                createdBy,
                createdAt,
                updatedAt
        );
    }

    public boolean isActive() {
        return this.status == TimeOffStatus.ACTIVE;
    }

    public boolean overlaps(Instant start, Instant end) {
        return isActive() && this.startTime.isBefore(end) && this.endTime.isAfter(start);
    }

    public void cancel(Instant now) {
        if (this.status == TimeOffStatus.CANCELLED) {
            return;
        }
        this.status = TimeOffStatus.CANCELLED;
        this.updatedAt = Objects.requireNonNull(now, "Cancelled at cannot be null.");
    }
}
