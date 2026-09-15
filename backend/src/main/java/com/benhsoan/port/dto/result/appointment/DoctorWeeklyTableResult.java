package com.benhsoan.port.dto.result.appointment;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.appointment.enums.SlotAvailabilityStatus;

import lombok.Builder;

@Builder
public record DoctorWeeklyTableResult(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        LocalTime clinicStartTime,
        LocalTime clinicEndTime,
        List<LocalTime> timeSlots,
        List<DoctorSummaryResult> doctors,
        List<DoctorDayScheduleResult> days
) {

    @Builder
    public record DoctorSummaryResult(
            UUID id,
            String fullName,
            String username,
            String specialtyName
    ) {
    }

    @Builder
    public record DoctorDayScheduleResult(
            LocalDate date,
            DayOfWeek dayOfWeek,
            List<DoctorScheduleDayResult> doctorSchedules
    ) {
    }

    @Builder
    public record DoctorScheduleDayResult(
            UUID doctorId,
            String doctorName,
            boolean workingDay,
            LocalTime workingStartTime,
            LocalTime workingEndTime,
            List<DoctorScheduleSlotResult> slots,
            List<DoctorTimeOffSummaryResult> timeOffs
    ) {
    }

    @Builder
    public record DoctorScheduleSlotResult(
            Instant startTime,
            Instant endTime,
            LocalTime slotStartTime,
            LocalTime slotEndTime,
            SlotAvailabilityStatus status,
            boolean isBookable,
            AppointmentSummaryResult appointment,
            String timeOffReason
    ) {
    }

    @Builder
    public record AppointmentSummaryResult(
            UUID id,
            String appointmentCode,
            UUID patientId,
            String patientCode,
            String patientName,
            String patientPhone,
            AppointmentStatus status,
            String reason
    ) {
    }

    @Builder
    public record DoctorTimeOffSummaryResult(
            UUID id,
            Instant startTime,
            Instant endTime,
            String reason
    ) {
    }
}
