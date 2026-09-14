package com.benhsoan.adapter.inbound.rest.response.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

import lombok.Builder;

@Builder
public record AppointmentResponse(

        UUID id,

        String appointmentCode,

        UUID patientId,

        UUID doctorId,

        Instant startTime,

        Instant endTime,

        AppointmentStatus status,

        String reason,

        String cancelReason,

        Instant checkedInAt,

        Instant completedAt,

        Instant createdAt,

        List<AppointmentRescheduleHistoryResponse> rescheduleHistories

) {
    public AppointmentResponse {
        if (rescheduleHistories == null) {
            rescheduleHistories = List.of();
        }
    }
}