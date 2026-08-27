package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppliedMedicalRecordTemplateResponse(
        UUID templateId,
        UUID templateVersionId,
        SpecialtyResponse specialty,
        String name,
        int versionNo,
        List<MedicalRecordTemplateSectionResponse> sections,
        UUID appliedBy,
        Instant appliedAt,
        boolean fallback
) {
}
