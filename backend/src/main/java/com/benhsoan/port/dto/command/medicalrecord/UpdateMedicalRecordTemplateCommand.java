package com.benhsoan.port.dto.command.medicalrecord;

import java.util.List;
import java.util.UUID;

public record UpdateMedicalRecordTemplateCommand(
        UUID templateId,
        String name,
        List<MedicalRecordTemplateSectionCommand> sections,
        String changeNote
) {
}
