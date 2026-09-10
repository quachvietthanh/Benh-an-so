package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

public record CancelPrescriptionCommand(
        UUID prescriptionId,
        String cancelReason
) {
}
