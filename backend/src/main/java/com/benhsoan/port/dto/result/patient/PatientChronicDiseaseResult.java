package com.benhsoan.port.dto.result.patient;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record PatientChronicDiseaseResult(
        UUID id,
        UUID patientId,
        UUID diagnosisCatalogId,
        Integer yearDetected,
        String notes,
        boolean active,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt
) {
}
