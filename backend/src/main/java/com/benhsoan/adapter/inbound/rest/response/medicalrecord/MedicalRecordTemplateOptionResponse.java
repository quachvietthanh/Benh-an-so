package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.util.List;
import java.util.UUID;

public record MedicalRecordTemplateOptionResponse(
        UUID templateId,
        UUID templateVersionId,
        SpecialtyResponse specialty,
        String name,
        int versionNo,
        boolean defaultTemplate,
        List<MedicalRecordTemplateSectionResponse> sections
) {
}
