package com.benhsoan.domain.appointment;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A doctor's recurring weekly working schedule for a specific day of the week (NCL-03-CN-006 / QTN-30).
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DoctorWeeklySchedule {

    private UUID id;

    private UUID doctorId;

    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    private DoctorWeeklySchedule(
            UUID id,
            UUID doctorId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.doctorId = Guard.require(doctorId, "Doctor id");
        this.dayOfWeek = Guard.require(dayOfWeek, "Day of week");
        this.startTime = Guard.require(startTime, "Start time");
        this.endTime = Guard.require(endTime, "End time");

        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("Schedule end time must be after start time.");
        }

        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = updatedAt;
    }

    public static DoctorWeeklySchedule create(
            UUID doctorId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Instant now
    ) {
        return new DoctorWeeklySchedule(
                UUID.randomUUID(),
                doctorId,
                dayOfWeek,
                startTime,
                endTime,
                true,
                Objects.requireNonNull(now, "Created at must not be null"),
                null
        );
    }

    @Deprecated
    public static DoctorWeeklySchedule create(
            UUID doctorId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return create(doctorId, dayOfWeek, startTime, endTime, Instant.now());
    }

    public static DoctorWeeklySchedule restore(
            UUID id,
            UUID doctorId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new DoctorWeeklySchedule(
                id,
                doctorId,
                dayOfWeek,
                startTime,
                endTime,
                active,
                createdAt,
                updatedAt
        );
    }

    public void update(LocalTime startTime, LocalTime endTime, boolean active, Instant updatedAt) {
        Guard.require(startTime, "Start time");
        Guard.require(endTime, "End time");
        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("Schedule end time must be after start time.");
        }
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }
}
