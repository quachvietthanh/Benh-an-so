package com.benhsoan.port.dto.result.appointment;

import java.util.List;
import java.util.UUID;

public record DoctorWeeklyScheduleResult(
        UUID doctorId,
        List<DoctorWeeklyScheduleItemResult> items
) {
}
