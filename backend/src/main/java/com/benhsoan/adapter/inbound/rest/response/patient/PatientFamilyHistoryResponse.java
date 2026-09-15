package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.Instant;
import java.util.UUID;

public record PatientFamilyHistoryResponse(
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
