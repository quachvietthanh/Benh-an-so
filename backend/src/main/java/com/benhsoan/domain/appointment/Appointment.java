package com.benhsoan.domain.appointment;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.exception.AppointmentAlreadyCancelledException;
import com.benhsoan.domain.appointment.exception.AppointmentAlreadyCompletedException;
import com.benhsoan.domain.appointment.exception.AppointmentInvalidStatusException;
import com.benhsoan.domain.appointment.exception.AppointmentNotOverdueException;
import com.benhsoan.domain.appointment.exception.AppointmentTimeInPastException;
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
public class Appointment {

    private UUID id;

    private String appointmentCode;

    private UUID patientId;

    private UUID doctorId;

    private Instant startTime;

    private Instant endTime;

    private AppointmentStatus status;

    private String reason;

    private String cancelReason;

    private Instant checkedInAt;

    private Instant completedAt;

    private UUID createdBy;

    private Instant createdAt;

    private static final Duration NO_SHOW_THRESHOLD = Duration.ofMinutes(15);
    
    private Appointment(
            UUID id,
            String appointmentCode,
            UUID patientId,
            UUID doctorId,
            Instant startTime, 
            Instant endTime,
            AppointmentStatus status,
            String reason,
            String cancelReason,
            Instant checkedInAt,
            Instant completedAt,
            UUID createdBy,
            Instant createdAt
    ) {

        this.id = Objects.requireNonNull(id);
        this.appointmentCode = Guard.require(appointmentCode, "Appointment code");
        this.patientId = Objects.requireNonNull(patientId);
        this.doctorId = Objects.requireNonNull(doctorId);
        this.startTime = Guard.require(startTime, "Start time");
        this.endTime = Guard.require(endTime, "End time");
        this.status = Guard.require(status, "Status");
        this.reason = Guard.require(reason, "Reason");
        this.cancelReason = cancelReason;
        this.checkedInAt = checkedInAt;
        this.completedAt = completedAt;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static Appointment create(
            String appointmentCode,
            UUID patientId,
            UUID doctorId,
            Instant startTime,
            Instant endTime,
            String reason,
            UUID createdBy
    ) {
         if (!endTime.isAfter(startTime)) 
            throw new ValidationException("End time must be after start time.");
        if (startTime.isBefore(Instant.now()))
            throw new AppointmentTimeInPastException();

        return new Appointment(
                UUID.randomUUID(),
                appointmentCode,
                patientId,
                doctorId,
                startTime,
                endTime,
                AppointmentStatus.SCHEDULED,
                reason,
                null,
                null,
                null,
                createdBy,
                Instant.now()
        );
    }

        public static Appointment restore(
            UUID id,
            String appointmentCode,
            UUID patientId,
            UUID doctorId,
            Instant startTime,
            Instant endTime,
            AppointmentStatus status,
            String reason,
            String cancelReason,
            Instant checkedInAt,
            Instant completedAt,
            UUID createdBy,
            Instant createdAt
    ) {
        return new Appointment(
                id,
                appointmentCode,
                patientId,
                doctorId,
                startTime,
                endTime,
                status,
                reason,
                cancelReason,
                checkedInAt,
                completedAt,
                createdBy,
                createdAt
        );
    }

    public void reschedule(
            UUID doctorId,
            Instant startTime,
            Instant endTime,
            String reason ) {

         if (!endTime.isAfter(startTime)) 
            throw new ValidationException("End time must be after start time.");
        if (startTime.isBefore(Instant.now()))
            throw new AppointmentTimeInPastException();
        
        if (status == AppointmentStatus.CANCELLED) {
            throw new AppointmentAlreadyCancelledException();
        }

        if (status == AppointmentStatus.COMPLETED) {
            throw new AppointmentAlreadyCompletedException();
        }

        if (endTime.isBefore(Instant.now())) {
            throw new AppointmentTimeInPastException();
        }

        this.doctorId = Objects.requireNonNull(doctorId);
        this.startTime = Guard.require(startTime, "Start time");
        this.endTime = Guard.require(endTime, "End time");
        this.reason = Guard.require(reason, "Reason");
    }

    public void checkIn(Instant checkedInAt) {
        if (status != AppointmentStatus.SCHEDULED) {
            throw new AppointmentInvalidStatusException(
                    "Only scheduled appointments can be checked in."
            );
        }

        this.checkedInAt = Guard.require(checkedInAt, "Checked in time");
        this.status = AppointmentStatus.CHECKED_IN;
    }

    public void start() {
        if (status != AppointmentStatus.CHECKED_IN) {
            throw new AppointmentInvalidStatusException(
                    "Only checked in appointments can be started."
            );
        }

        this.status = AppointmentStatus.IN_PROGRESS;
    }

    public void complete(Instant completedAt) {
        if (status != AppointmentStatus.IN_PROGRESS) {
            throw new AppointmentInvalidStatusException(
                    "Only appointments in progress can be completed."
            );
        }

        this.completedAt = Guard.require(completedAt, "Completed time");
        this.status = AppointmentStatus.COMPLETED;
    }

    public void cancel(String cancelReason) {
        if (status == AppointmentStatus.CANCELLED) {
            throw new AppointmentAlreadyCancelledException();
        }

        if (status == AppointmentStatus.COMPLETED) {
            throw new AppointmentAlreadyCompletedException();
        }
        
        this.cancelReason = Guard.require(cancelReason, "Cancel reason");
        this.status = AppointmentStatus.CANCELLED;
    }

    public void markNoShow(Instant now) {

        Guard.require(now, "Current time");

        if (status != AppointmentStatus.SCHEDULED) {
            throw new AppointmentInvalidStatusException(
                    "Only scheduled appointments can be marked as no show."
            );
        }

        if (now.isBefore(getNoShowThresholdTime())) {
            throw new AppointmentNotOverdueException();
        }

        this.status = AppointmentStatus.NO_SHOW;
    }

    public boolean canReschedule() {
        return !isFinished();
    }

    public boolean canCheckIn() {
        return status == AppointmentStatus.SCHEDULED;
    }

    public boolean canStart() {
        return status == AppointmentStatus.CHECKED_IN;
    }

    public boolean canComplete() {
        return status == AppointmentStatus.IN_PROGRESS;
    }

    public boolean canCancel() {
        return !isFinished();
    }

    public boolean canMarkNoShow(Instant now) {
        Guard.require(now, "Current time");
        return status == AppointmentStatus.SCHEDULED
                && !now.isBefore(getNoShowThresholdTime());
    }

    public boolean isScheduled() {
        return status == AppointmentStatus.SCHEDULED;
    }

    public boolean isFinished() {
        return status == AppointmentStatus.COMPLETED
            || status == AppointmentStatus.CANCELLED
            || status == AppointmentStatus.NO_SHOW;
    }

    private Instant getNoShowThresholdTime() {
        return startTime.plus(NO_SHOW_THRESHOLD);
    }
}