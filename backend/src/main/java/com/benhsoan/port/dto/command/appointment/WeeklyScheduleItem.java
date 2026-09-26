package com.benhsoan.port.dto.command.appointment;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WeeklyScheduleItem(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {
}
