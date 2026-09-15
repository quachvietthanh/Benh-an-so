package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.Instant;
import java.util.UUID;

public record PatientChronicDiseaseResponse(
        UUID id,
        UUID patientId,
        UUID diagnosisCatalogId,
        String diagnosisCode,
        String diagnosisName,
        Integer yearDetected,
        String notes,
        boolean active,
        UUID createdBy,
        Instant createdAt,
        UUID updatedBy,
        Instant updatedAt
) {
}
