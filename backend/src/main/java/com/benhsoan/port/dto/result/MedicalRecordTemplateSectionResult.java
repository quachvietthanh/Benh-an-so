package com.benhsoan.port.dto.result;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;

public record MedicalRecordTemplateSectionResult(
        MedicalRecordFieldCode fieldCode,
        String label,
        boolean required,
        int displayOrder
) {
}
