package com.benhsoan.port.dto.result.appointment;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record DoctorWeeklyScheduleResult(
        UUID id,
        UUID doctorId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {
}
