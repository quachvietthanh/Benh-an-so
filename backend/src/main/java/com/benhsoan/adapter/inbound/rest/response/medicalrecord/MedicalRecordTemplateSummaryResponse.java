package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.UUID;

public record MedicalRecordTemplateSummaryResponse(
        UUID id,
        SpecialtyResponse specialty,
        String name,
        boolean active,
        boolean defaultTemplate,
        int currentVersionNo,
        Instant createdAt,
        Instant updatedAt
) {
}
