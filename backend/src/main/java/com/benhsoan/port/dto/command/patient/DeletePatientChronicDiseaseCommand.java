package com.benhsoan.port.dto.command.patient;

import java.util.UUID;

import lombok.Builder;

@Builder
public record DeletePatientChronicDiseaseCommand(
        UUID chronicDiseaseId,
        UUID patientId,
        String reason
) {
}
