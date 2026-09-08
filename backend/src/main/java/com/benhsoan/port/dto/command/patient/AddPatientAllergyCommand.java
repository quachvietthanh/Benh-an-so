package com.benhsoan.port.dto.command.patient;

import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;

import lombok.Builder;

@Builder
public record AddPatientAllergyCommand(
        UUID patientId,
        String allergenType,
        String allergenName,
        AllergySeverity severity,
        String reaction,
        String notes,
        UUID visitId
) {
}
