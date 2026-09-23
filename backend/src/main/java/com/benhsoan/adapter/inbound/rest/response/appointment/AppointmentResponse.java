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

        Instant confirmedAt,

        UUID confirmedBy,

        String confirmedByName,

        List<AppointmentRescheduleHistoryResponse> rescheduleHistories,

        WaitlistSuggestionResponse suggestedWaitlistEntry

) {
    public AppointmentResponse {
        if (rescheduleHistories == null) {
            rescheduleHistories = List.of();
        }
    }

    public AppointmentResponse(
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
        this(id, appointmentCode, patientId, doctorId, startTime, endTime, status,
                reason, cancelReason, checkedInAt, completedAt, createdAt, null, null, null,
                rescheduleHistories != null ? rescheduleHistories : List.of(), null);
    }

    public AppointmentResponse(
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
            Instant confirmedAt,
            UUID confirmedBy,
            String confirmedByName,
            List<AppointmentRescheduleHistoryResponse> rescheduleHistories
    ) {
        this(id, appointmentCode, patientId, doctorId, startTime, endTime, status,
                reason, cancelReason, checkedInAt, completedAt, createdAt, confirmedAt,
                confirmedBy, confirmedByName, rescheduleHistories != null ? rescheduleHistories : List.of(), null);
    }
}