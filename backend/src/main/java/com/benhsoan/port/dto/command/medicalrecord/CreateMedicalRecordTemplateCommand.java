package com.benhsoan.port.dto.command.medicalrecord;

import java.util.List;
import java.util.UUID;

public record CreateMedicalRecordTemplateCommand(
        UUID specialtyId,
        String name,
        boolean makeDefault,
        List<MedicalRecordTemplateSectionCommand> sections
) {
}
