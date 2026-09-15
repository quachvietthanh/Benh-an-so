package com.benhsoan.port.dto.command.patient;

import java.util.UUID;

import lombok.Builder;

@Builder
public record AddPatientChronicDiseaseCommand(
        UUID patientId,
        UUID diagnosisCatalogId,
        Integer yearDetected,
        String notes,
        UUID visitId
) {
}
