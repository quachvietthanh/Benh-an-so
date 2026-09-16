package com.benhsoan.port.dto.command.patient;

import java.util.UUID;

import lombok.Builder;

@Builder
public record AddPatientFamilyHistoryCommand(
        UUID patientId,
        String relationship,
        UUID diagnosisCatalogId,
        String notes,
        UUID visitId
) {
}
