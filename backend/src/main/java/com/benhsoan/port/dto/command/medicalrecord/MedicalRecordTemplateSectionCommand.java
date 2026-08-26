package com.benhsoan.port.dto.command.medicalrecord;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;

public record MedicalRecordTemplateSectionCommand(
        MedicalRecordFieldCode fieldCode,
        String label,
        boolean required,
        int displayOrder
) {
}
