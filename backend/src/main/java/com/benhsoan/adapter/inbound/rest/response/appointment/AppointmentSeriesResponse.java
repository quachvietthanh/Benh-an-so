package com.benhsoan.adapter.inbound.rest.response.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentSeriesStatus;

import lombok.Builder;

@Builder
public record AppointmentSeriesResponse(
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
        List<AppointmentResponse> appointments
) {
    public AppointmentSeriesResponse {
        if (appointments == null) {
            appointments = List.of();
        }
    }
}
