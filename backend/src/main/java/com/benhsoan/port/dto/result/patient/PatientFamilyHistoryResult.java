package com.benhsoan.port.dto.result.patient;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record PatientFamilyHistoryResult(
        UUID id,
        UUID patientId,
        String relationship,
        UUID diagnosisCatalogId,
        String notes,
        boolean active,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt
) {
}
