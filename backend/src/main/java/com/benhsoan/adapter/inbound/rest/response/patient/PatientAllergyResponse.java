package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;

public record PatientAllergyResponse(
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
