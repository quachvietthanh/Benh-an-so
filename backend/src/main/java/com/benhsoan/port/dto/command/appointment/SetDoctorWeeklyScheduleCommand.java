package com.benhsoan.port.dto.command.appointment;

import java.util.List;
import java.util.UUID;

public record SetDoctorWeeklyScheduleCommand(
        UUID doctorId,
        List<DoctorWeeklyScheduleItemCommand> items
) {
}
