package com.benhsoan.adapter.inbound.rest.request.carelog;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostCareLogRequest(
        @NotNull(message = "patientId is required.")
        UUID patientId,

        UUID reminderId,

        UUID visitId,

        @NotNull(message = "contactChannel is required.")
        ContactChannel contactChannel,

        @NotNull(message = "contactedAt is required.")
        Instant contactedAt,

        @NotNull(message = "patientCondition is required.")
        PatientCondition patientCondition,

        @NotBlank(message = "careNotes is required.")
        @Size(max = 2000, message = "careNotes must not exceed 2000 characters.")
        String careNotes,

        @NotNull(message = "contactOutcome is required.")
        ContactOutcome contactOutcome
) {
}
