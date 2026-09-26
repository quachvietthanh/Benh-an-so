package com.benhsoan.port.dto.command.patient;

import java.util.UUID;

import lombok.Builder;

@Builder
public record DeletePatientAllergyCommand(
        UUID allergyId,
        UUID patientId,
        String reason
) {
}
