package com.benhsoan.adapter.inbound.rest.response.appointment;

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
public record DoctorWeeklyTableResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        LocalTime clinicStartTime,
        LocalTime clinicEndTime,
        List<LocalTime> timeSlots,
        List<DoctorSummaryResponse> doctors,
        List<DoctorDayScheduleResponse> days
) {

    @Builder
    public record DoctorSummaryResponse(
            UUID id,
            String fullName,
            String username,
            String specialtyName
    ) {
    }

    @Builder
    public record DoctorDayScheduleResponse(
            LocalDate date,
            DayOfWeek dayOfWeek,
            List<DoctorScheduleDayResponse> doctorSchedules
    ) {
    }

    @Builder
    public record DoctorScheduleDayResponse(
            UUID doctorId,
            String doctorName,
            boolean workingDay,
            LocalTime workingStartTime,
            LocalTime workingEndTime,
            List<DoctorScheduleSlotResponse> slots,
            List<DoctorTimeOffSummaryResponse> timeOffs
    ) {
    }

    @Builder
    public record DoctorScheduleSlotResponse(
            Instant startTime,
            Instant endTime,
            LocalTime slotStartTime,
            LocalTime slotEndTime,
            SlotAvailabilityStatus status,
            boolean isBookable,
            AppointmentSummaryResponse appointment,
            String timeOffReason
    ) {
    }

    @Builder
    public record AppointmentSummaryResponse(
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
    public record DoctorTimeOffSummaryResponse(
            UUID id,
            Instant startTime,
            Instant endTime,
            String reason
    ) {
    }
}
