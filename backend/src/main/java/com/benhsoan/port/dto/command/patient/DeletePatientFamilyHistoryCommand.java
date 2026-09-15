package com.benhsoan.port.dto.command.patient;

import java.util.UUID;

import lombok.Builder;

@Builder
public record DeletePatientFamilyHistoryCommand(
        UUID familyHistoryId,
        UUID patientId,
        String reason
) {
}
