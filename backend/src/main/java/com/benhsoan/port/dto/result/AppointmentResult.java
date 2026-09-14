package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.dto.result.appointment.AppointmentRescheduleHistoryResult;

import lombok.Builder;

@Builder
public record AppointmentResult(

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

        UUID createdBy,

        Instant createdAt,

        List<AppointmentRescheduleHistoryResult> rescheduleHistories

) {
    public AppointmentResult(
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
            UUID createdBy,
            Instant createdAt
    ) {
        this(id, appointmentCode, patientId, doctorId, startTime, endTime, status,
                reason, cancelReason, checkedInAt, completedAt, createdBy, createdAt, List.of());
    }
}