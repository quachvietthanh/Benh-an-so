package com.benhsoan.port.dto.result.appointment;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record AppointmentRescheduleHistoryResult(
        UUID id,
        UUID appointmentId,
        UUID oldDoctorId,
        UUID newDoctorId,
        String oldDoctorName,
        String newDoctorName,
        Instant oldStartTime,
        Instant oldEndTime,
        Instant newStartTime,
        Instant newEndTime,
        String reason,
        UUID rescheduledBy,
        String rescheduledByName,
        Instant rescheduledAt
) {
}
