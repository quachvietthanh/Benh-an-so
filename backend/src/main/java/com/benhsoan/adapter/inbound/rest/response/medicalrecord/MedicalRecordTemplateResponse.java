package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MedicalRecordTemplateResponse(
        UUID id,
        SpecialtyResponse specialty,
        String name,
        boolean active,
        boolean defaultTemplate,
        int currentVersionNo,
        List<MedicalRecordTemplateSectionResponse> sections,
        Instant createdAt,
        Instant updatedAt
) {
}
