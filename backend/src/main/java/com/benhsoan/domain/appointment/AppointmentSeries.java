package com.benhsoan.domain.appointment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentSeriesStatus;
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
public class AppointmentSeries {

    private UUID id;
    private String seriesCode;
    private UUID patientId;
    private UUID doctorId;
    private UUID medicalRecordId;
    private int totalSessions;
    private int intervalDays;
    private String title;
    private String notes;
    private AppointmentSeriesStatus status;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    private AppointmentSeries(
            UUID id,
            String seriesCode,
            UUID patientId,
            UUID doctorId,
            UUID medicalRecordId,
            int totalSessions,
            int intervalDays,
            String title,
            String notes,
            AppointmentSeriesStatus status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "ID liệu trình không được để trống");
        this.seriesCode = Guard.require(seriesCode, "Mã liệu trình");
        this.patientId = Objects.requireNonNull(patientId, "ID bệnh nhân không được để trống");
        this.doctorId = Objects.requireNonNull(doctorId, "ID bác sĩ không được để trống");
        this.medicalRecordId = medicalRecordId;
        this.totalSessions = totalSessions;
        this.intervalDays = intervalDays;
        this.title = Guard.require(title, "Tiêu đề liệu trình");
        this.notes = notes;
        this.status = Objects.requireNonNull(status, "Trạng thái liệu trình không được để trống");
        this.createdBy = Objects.requireNonNull(createdBy, "Người tạo không được để trống");
        this.createdAt = Objects.requireNonNull(createdAt, "Thời gian tạo không được để trống");
        this.updatedAt = updatedAt;
    }

    public static AppointmentSeries create(
            String seriesCode,
            UUID patientId,
            UUID doctorId,
            UUID medicalRecordId,
            int totalSessions,
            int intervalDays,
            String title,
            String notes,
            UUID createdBy,
            Instant now
    ) {
        if (totalSessions < 2) {
            throw new ValidationException("Số buổi của liệu trình phải từ 2 trở lên.");
        }
        if (intervalDays < 1) {
            throw new ValidationException("Khoảng cách giữa các buổi phải từ 1 ngày trở lên.");
        }

        return new AppointmentSeries(
                UUID.randomUUID(),
                seriesCode,
                patientId,
                doctorId,
                medicalRecordId,
                totalSessions,
                intervalDays,
                title,
                notes,
                AppointmentSeriesStatus.ACTIVE,
                createdBy,
                now,
                null
        );
    }

    public static AppointmentSeries restore(
            UUID id,
            String seriesCode,
            UUID patientId,
            UUID doctorId,
            UUID medicalRecordId,
            int totalSessions,
            int intervalDays,
            String title,
            String notes,
            AppointmentSeriesStatus status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new AppointmentSeries(
                id,
                seriesCode,
                patientId,
                doctorId,
                medicalRecordId,
                totalSessions,
                intervalDays,
                title,
                notes,
                status,
                createdBy,
                createdAt,
                updatedAt
        );
    }

    public void complete(Instant now) {
        this.status = AppointmentSeriesStatus.COMPLETED;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        this.status = AppointmentSeriesStatus.CANCELLED;
        this.updatedAt = now;
    }
}
