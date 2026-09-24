package com.benhsoan.adapter.inbound.rest.response.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.TimePreference;
import com.benhsoan.domain.appointment.enums.WaitlistStatus;

import lombok.Builder;

@Builder
public record AppointmentWaitlistResponse(
        UUID id,
        UUID patientId,
        String patientName,
        String patientPhone,
        UUID doctorId,
        String doctorName,
        LocalDate desiredDate,
        TimePreference timePreference,
        WaitlistStatus status,
        String note,
        String cancelReason,
        UUID bookedAppointmentId,
        UUID createdBy,
        String createdByName,
        Instant createdAt,
        Instant updatedAt
) {
}
