package com.benhsoan.domain.appointment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentRescheduleLog {

    private UUID id;
    private UUID appointmentId;
    private UUID oldDoctorId;
    private UUID newDoctorId;
    private Instant oldStartTime;
    private Instant oldEndTime;
    private Instant newStartTime;
    private Instant newEndTime;
    private String reason;
    private UUID rescheduledBy;
    private Instant rescheduledAt;

    private AppointmentRescheduleLog(
            UUID id,
            UUID appointmentId,
            UUID oldDoctorId,
            UUID newDoctorId,
            Instant oldStartTime,
            Instant oldEndTime,
            Instant newStartTime,
            Instant newEndTime,
            String reason,
            UUID rescheduledBy,
            Instant rescheduledAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.appointmentId = Objects.requireNonNull(appointmentId);
        this.oldDoctorId = Objects.requireNonNull(oldDoctorId);
        this.newDoctorId = Objects.requireNonNull(newDoctorId);
        this.oldStartTime = Guard.require(oldStartTime, "Old start time");
        this.oldEndTime = Guard.require(oldEndTime, "Old end time");
        this.newStartTime = Guard.require(newStartTime, "New start time");
        this.newEndTime = Guard.require(newEndTime, "New end time");
        this.reason = Guard.require(reason, "Reason");
        this.rescheduledBy = Objects.requireNonNull(rescheduledBy);
        this.rescheduledAt = Objects.requireNonNull(rescheduledAt);
    }

    public static AppointmentRescheduleLog create(
            UUID appointmentId,
            UUID oldDoctorId,
            UUID newDoctorId,
            Instant oldStartTime,
            Instant oldEndTime,
            Instant newStartTime,
            Instant newEndTime,
            String reason,
            UUID rescheduledBy,
            Instant rescheduledAt
    ) {
        return new AppointmentRescheduleLog(
                UUID.randomUUID(),
                appointmentId,
                oldDoctorId,
                newDoctorId,
                oldStartTime,
                oldEndTime,
                newStartTime,
                newEndTime,
                reason,
                rescheduledBy,
                rescheduledAt
        );
    }

    public static AppointmentRescheduleLog restore(
            UUID id,
            UUID appointmentId,
            UUID oldDoctorId,
            UUID newDoctorId,
            Instant oldStartTime,
            Instant oldEndTime,
            Instant newStartTime,
            Instant newEndTime,
            String reason,
            UUID rescheduledBy,
            Instant rescheduledAt
    ) {
        return new AppointmentRescheduleLog(
                id,
                appointmentId,
                oldDoctorId,
                newDoctorId,
                oldStartTime,
                oldEndTime,
                newStartTime,
                newEndTime,
                reason,
                rescheduledBy,
                rescheduledAt
        );
    }
}
