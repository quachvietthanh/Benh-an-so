package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MedicalRecordTemplateResult(
        UUID id,
        SpecialtyResult specialty,
        String name,
        boolean active,
        boolean defaultTemplate,
        int currentVersionNo,
        List<MedicalRecordTemplateSectionResult> sections,
        Instant createdAt,
        Instant updatedAt
) {
}
