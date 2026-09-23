package com.benhsoan.port.dto.result.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentSeriesStatus;
import com.benhsoan.port.dto.result.AppointmentResult;

import lombok.Builder;

@Builder
public record AppointmentSeriesResult(
        UUID id,
        String seriesCode,
        UUID patientId,
        UUID doctorId,
        UUID medicalRecordId,
        int totalSessions,
        int intervalDays,
        String title,
        String notes,
        AppointmentSeriesStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<AppointmentResult> appointments
) {
}
