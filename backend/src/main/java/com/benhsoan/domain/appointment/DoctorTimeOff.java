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
 * A doctor's time-off / leave interval (NCL-03-CN-006 / QTN-30).
 * Blocks appointment bookings during this window.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DoctorTimeOff {

    private static final int MAX_REASON_LENGTH = 500;

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
        this.id = Objects.requireNonNull(id);
        this.doctorId = Guard.require(doctorId, "Doctor id");
        this.startTime = Guard.require(startTime, "Start time");
        this.endTime = Guard.require(endTime, "End time");

        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("Time-off end time must be after start time.");
        }

        this.reason = normalizeReason(reason);
        this.status = Objects.requireNonNullElse(status, TimeOffStatus.ACTIVE);
        this.createdBy = Guard.require(createdBy, "Created by");
        this.createdAt = Objects.requireNonNull(createdAt);
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
        Guard.require(now, "Current time");
        if (startTime.isBefore(now)) {
            throw new ValidationException("Thời gian nghỉ không được bắt đầu trong quá khứ.");
        }

        return new DoctorTimeOff(
                UUID.randomUUID(),
                doctorId,
                startTime,
                endTime,
                reason,
                TimeOffStatus.ACTIVE,
                createdBy,
                now,
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

    public void cancel(Instant now) {
        if (this.status == TimeOffStatus.CANCELLED) {
            throw new ValidationException("Khoảng nghỉ đã bị hủy trước đó.");
        }
        this.status = TimeOffStatus.CANCELLED;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public boolean isActive() {
        return this.status == TimeOffStatus.ACTIVE;
    }

    public boolean overlaps(Instant otherStart, Instant otherEnd) {
        if (!isActive()) {
            return false;
        }
        return this.startTime.isBefore(otherEnd) && this.endTime.isAfter(otherStart);
    }

    private static String normalizeReason(String reason) {
        String trimmed = Guard.require(reason, "Reason").trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            throw new ValidationException("Lý do nghỉ không được để trống.");
        }
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new ValidationException("Lý do nghỉ không được vượt quá " + MAX_REASON_LENGTH + " ký tự.");
        }
        return trimmed;
    }
}
