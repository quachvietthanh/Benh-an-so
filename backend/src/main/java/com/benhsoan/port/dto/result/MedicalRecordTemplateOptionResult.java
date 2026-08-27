package com.benhsoan.port.dto.result;

import java.util.List;
import java.util.UUID;

public record MedicalRecordTemplateOptionResult(
        UUID templateId,
        UUID templateVersionId,
        SpecialtyResult specialty,
        String name,
        int versionNo,
        boolean defaultTemplate,
        List<MedicalRecordTemplateSectionResult> sections
) {
}
