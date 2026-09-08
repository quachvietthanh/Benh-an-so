package com.benhsoan.adapter.inbound.rest.response.appointment;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record DoctorWeeklyScheduleItemResponse(
        UUID id,
        UUID doctorId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {
}
