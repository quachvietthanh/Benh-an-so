package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

public record PrescriptionAllergyOverrideCommand(
        UUID allergyId,
        UUID medicineId,
        String overrideReason
) {
}
