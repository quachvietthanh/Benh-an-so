package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

/**
 * Command for full dispensing of a prescription. {@code controlledMedicineConfirmed}
 * is the server-side confirmation required when the prescription contains a
 * controlled medicine (QTN-39, NCL-06-CN-014).
 */
public record DispensePrescriptionCommand(
        UUID prescriptionId,
        boolean controlledMedicineConfirmed
) {
    public DispensePrescriptionCommand(UUID prescriptionId) {
        this(prescriptionId, false);
    }
}
