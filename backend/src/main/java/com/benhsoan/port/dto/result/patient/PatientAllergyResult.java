package com.benhsoan.port.dto.result.patient;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;

import lombok.Builder;

@Builder
public record PatientAllergyResult(
        UUID id,
        UUID patientId,
        String allergenType,
        String allergenName,
        AllergySeverity severity,
        String reaction,
        String notes,
        boolean active,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt
) {
}
