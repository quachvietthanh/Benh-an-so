package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

public record SavePrescriptionTemplateCommand(
        UUID prescriptionId,
        String diagnosisCode
) {
}
