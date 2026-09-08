package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record DoctorWeeklyScheduleItemRequest(
        @NotNull(message = "dayOfWeek is required.")
        DayOfWeek dayOfWeek,

        @NotNull(message = "startTime is required.")
        LocalTime startTime,

        @NotNull(message = "endTime is required.")
        LocalTime endTime,

        Boolean active
) {
}
