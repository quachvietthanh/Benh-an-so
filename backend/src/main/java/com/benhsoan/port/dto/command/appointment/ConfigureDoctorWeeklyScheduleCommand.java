package com.benhsoan.port.dto.command.appointment;

import java.util.List;
import java.util.UUID;

public record ConfigureDoctorWeeklyScheduleCommand(
        UUID doctorId,
        List<WeeklyScheduleItem> schedules
) {
}
