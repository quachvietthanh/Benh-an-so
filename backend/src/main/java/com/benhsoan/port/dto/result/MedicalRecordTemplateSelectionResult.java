package com.benhsoan.port.dto.result;

import java.util.List;
import java.util.UUID;

public record MedicalRecordTemplateSelectionResult(
        UUID medicalRecordId,
        UUID visitId,
        SpecialtyResult visitSpecialty,
        List<MedicalRecordTemplateOptionResult> availableTemplates,
        MedicalRecordTemplateOptionResult effectiveTemplate,
        boolean fallback
) {
}
