package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

public record ApplyPrescriptionTemplateCommand(
        UUID templateId,
        UUID medicalRecordId
) {
}
