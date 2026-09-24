package com.benhsoan.port.dto.command.prescription;

public record PrescriptionMaxDailyDoseOverrideCommand(
        String activeIngredient,
        String overrideReason
) {
}
