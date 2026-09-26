package com.benhsoan.port.dto.result.patient;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record PatientAllergyChangeLogResult(
        UUID id,
        UUID allergyId,
        UUID patientId,
        String action,
        String beforeData,
        String afterData,
        String changeReason,
        UUID changedBy,
        Instant changedAt
) {
}
