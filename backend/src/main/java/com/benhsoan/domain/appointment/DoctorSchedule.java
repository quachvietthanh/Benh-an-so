package com.benhsoan.domain.appointment;

import java.time.Instant;
import java.time.LocalDate;
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
 * A doctor's working schedule for a single date (NCL-14-CN-003 / QTN-04).
 * Used to derive available appointment slots for the patient portal.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DoctorSchedule {

    private UUID id;

    private UUID doctorId;

    private LocalDate scheduleDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    private DoctorSchedule(
            UUID id,
            UUID doctorId,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.doctorId = Guard.require(doctorId, "Doctor id");
        this.scheduleDate = Guard.require(scheduleDate, "Schedule date");
        this.startTime = Guard.require(startTime, "Start time");
        this.endTime = Guard.require(endTime, "End time");

        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("Schedule end time must be after start time.");
        }

        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = updatedAt;
    }

    public static DoctorSchedule create(
            UUID doctorId,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return new DoctorSchedule(
                UUID.randomUUID(),
                doctorId,
                scheduleDate,
                startTime,
                endTime,
                true,
                Instant.now(),
                null
        );
    }

    public static DoctorSchedule restore(
            UUID id,
            UUID doctorId,
            LocalDate scheduleDate,
            LocalTime startTime,
            LocalTime endTime,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new DoctorSchedule(
                id,
                doctorId,
                scheduleDate,
                startTime,
                endTime,
                active,
                createdAt,
                updatedAt
        );
    }
}
