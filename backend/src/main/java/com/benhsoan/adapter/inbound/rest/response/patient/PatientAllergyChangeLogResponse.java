package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonRawValue;

public record PatientAllergyChangeLogResponse(
        UUID id,
        UUID allergyId,
        UUID patientId,
        String action,
        @JsonRawValue
        String beforeData,
        @JsonRawValue
        String afterData,
        String changeReason,
        UUID changedBy,
        Instant changedAt
) {
}
