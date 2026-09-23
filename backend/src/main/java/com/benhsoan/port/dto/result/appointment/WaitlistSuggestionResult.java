package com.benhsoan.port.dto.result.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.TimePreference;

import lombok.Builder;

@Builder
public record WaitlistSuggestionResult(
        UUID waitlistId,
        UUID patientId,
        String patientName,
        String patientPhone,
        UUID doctorId,
        String doctorName,
        LocalDate desiredDate,
        TimePreference timePreference,
        String note,
        Instant createdAt
) {
}
