package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

public record PrescriptionContraindicationOverrideCommand(
        UUID ruleId,
        UUID medicineId,
        String overrideReason
) {
}
