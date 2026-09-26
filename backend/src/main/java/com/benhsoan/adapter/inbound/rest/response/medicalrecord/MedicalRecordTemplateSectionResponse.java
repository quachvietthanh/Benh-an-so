package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;

public record MedicalRecordTemplateSectionResponse(
        MedicalRecordFieldCode fieldCode,
        String label,
        boolean required,
        int displayOrder
) {
}
