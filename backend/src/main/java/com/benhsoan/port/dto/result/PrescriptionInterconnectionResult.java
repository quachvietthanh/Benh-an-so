package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.prescription.enums.InterconnectionStatus;

public record PrescriptionInterconnectionResult(
        UUID prescriptionId,
        String prescriptionCode,
        InterconnectionStatus status,
        String receiptCode,
        String failureReason,
        Instant completedAt
) {
}
