package com.benhsoan.port.dto.query.appointment;

import java.time.LocalDate;
import java.util.UUID;

public record GetDoctorWeeklyScheduleTableQuery(
        LocalDate date,
        UUID doctorId
) {
}
