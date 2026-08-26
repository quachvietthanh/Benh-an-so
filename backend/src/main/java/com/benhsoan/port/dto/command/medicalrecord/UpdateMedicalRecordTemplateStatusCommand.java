package com.benhsoan.port.dto.command.medicalrecord;

import java.util.UUID;

public record UpdateMedicalRecordTemplateStatusCommand(
        UUID templateId,
        boolean active,
        UUID replacementTemplateId
) {
}
