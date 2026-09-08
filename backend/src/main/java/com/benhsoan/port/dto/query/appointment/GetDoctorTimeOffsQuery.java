package com.benhsoan.port.dto.query.appointment;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.TimeOffStatus;

public record GetDoctorTimeOffsQuery(
        UUID doctorId,
        TimeOffStatus status,
        Instant fromTime,
        Instant toTime
) {
}
