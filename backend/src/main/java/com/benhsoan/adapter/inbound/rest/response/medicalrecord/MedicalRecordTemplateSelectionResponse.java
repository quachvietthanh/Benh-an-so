package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.util.List;
import java.util.UUID;

public record MedicalRecordTemplateSelectionResponse(
        UUID medicalRecordId,
        UUID visitId,
        SpecialtyResponse visitSpecialty,
        List<MedicalRecordTemplateOptionResponse> availableTemplates,
        MedicalRecordTemplateOptionResponse effectiveTemplate,
        boolean fallback
) {
}
