package com.benhsoan.port.dto.command.patient;

import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;

import lombok.Builder;

@Builder
public record UpdatePatientAllergyCommand(
        UUID allergyId,
        UUID patientId,
        String allergenName,
        AllergySeverity severity,
        String reaction,
        String notes,
        String changeReason
) {
}
