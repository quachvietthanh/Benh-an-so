package com.benhsoan.port.dto.command.prescription;

import java.time.Instant;

import com.benhsoan.domain.prescription.enums.InterconnectionStatus;

public record SearchPrescriptionInterconnectionsQuery(
        InterconnectionStatus status,
        Instant from,
        Instant to,
        int page,
        int size
) {
}
