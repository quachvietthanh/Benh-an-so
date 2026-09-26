package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppliedMedicalRecordTemplateResult(
        UUID templateId,
        UUID templateVersionId,
        SpecialtyResult specialty,
        String name,
        int versionNo,
        List<MedicalRecordTemplateSectionResult> sections,
        UUID appliedBy,
        Instant appliedAt,
        boolean fallback
) {
}
