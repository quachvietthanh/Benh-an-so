package com.benhsoan.adapter.inbound.rest.response.appointment;

import java.util.List;
import java.util.UUID;

public record DoctorWeeklyScheduleResponse(
        UUID doctorId,
        List<DoctorWeeklyScheduleItemResponse> items
) {
}
