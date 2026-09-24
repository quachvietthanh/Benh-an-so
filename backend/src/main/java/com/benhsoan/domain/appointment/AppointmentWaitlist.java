package com.benhsoan.domain.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.TimePreference;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;
import com.benhsoan.domain.appointment.exception.WaitlistInvalidStatusException;
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
public class AppointmentWaitlist {

    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private LocalDate desiredDate;
    private TimePreference timePreference;
    private WaitlistStatus status;
    private String note;
    private String cancelReason;
    private UUID bookedAppointmentId;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    private AppointmentWaitlist(
            UUID id,
            UUID patientId,
            UUID doctorId,
            LocalDate desiredDate,
            TimePreference timePreference,
            WaitlistStatus status,
            String note,
            String cancelReason,
            UUID bookedAppointmentId,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "ID không được để trống");
        this.patientId = Objects.requireNonNull(patientId, "ID bệnh nhân không được để trống");
        this.doctorId = Objects.requireNonNull(doctorId, "ID bác sĩ không được để trống");
        this.desiredDate = Objects.requireNonNull(desiredDate, "Ngày mong muốn khám không được để trống");
        this.timePreference = timePreference != null ? timePreference : TimePreference.ANYTIME;
        this.status = status != null ? status : WaitlistStatus.WAITING;
        this.note = note;
        this.cancelReason = cancelReason;
        this.bookedAppointmentId = bookedAppointmentId;
        this.createdBy = Objects.requireNonNull(createdBy, "Người tạo không được để trống");
        this.createdAt = Objects.requireNonNull(createdAt, "Thời điểm tạo không được để trống");
        this.updatedAt = updatedAt;
    }

    public static AppointmentWaitlist create(
            UUID patientId,
            UUID doctorId,
            LocalDate desiredDate,
            TimePreference timePreference,
            String note,
            UUID createdBy,
            Instant now
    ) {
        return new AppointmentWaitlist(
                UUID.randomUUID(),
                patientId,
                doctorId,
                desiredDate,
                timePreference != null ? timePreference : TimePreference.ANYTIME,
                WaitlistStatus.WAITING,
                note,
                null,
                null,
                createdBy,
                now != null ? now : Instant.now(),
                null
        );
    }

    public static AppointmentWaitlist reconstitute(
            UUID id,
            UUID patientId,
            UUID doctorId,
            LocalDate desiredDate,
            TimePreference timePreference,
            WaitlistStatus status,
            String note,
            String cancelReason,
            UUID bookedAppointmentId,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new AppointmentWaitlist(
                id,
                patientId,
                doctorId,
                desiredDate,
                timePreference,
                status,
                note,
                cancelReason,
                bookedAppointmentId,
                createdBy,
                createdAt,
                updatedAt
        );
    }

    public void markScheduled(UUID appointmentId, Instant now) {
        if (this.status != WaitlistStatus.WAITING) {
            throw new WaitlistInvalidStatusException(
                    "Chỉ có thể chuyển sang trạng thái đã đặt lịch khi đang ở trạng thái WAITING.");
        }
        this.status = WaitlistStatus.SCHEDULED;
        this.bookedAppointmentId = Objects.requireNonNull(appointmentId, "ID lịch hẹn không được để trống");
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void cancel(String reason, Instant now) {
        if (this.status != WaitlistStatus.WAITING) {
            throw new WaitlistInvalidStatusException(
                    "Chỉ có thể hủy mục chờ khi đang ở trạng thái WAITING.");
        }
        this.cancelReason = Guard.require(reason, "Lý do hủy danh sách chờ");
        this.status = WaitlistStatus.CANCELLED;
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void markExpired(Instant now) {
        if (this.status == WaitlistStatus.WAITING) {
            this.status = WaitlistStatus.EXPIRED;
            this.updatedAt = now != null ? now : Instant.now();
        }
    }
}
