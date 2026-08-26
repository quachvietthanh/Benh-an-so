package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MedicalRecordTemplateSectionRequest(
        @NotNull MedicalRecordFieldCode fieldCode,
        @NotBlank String label,
        boolean required,
        @Positive int displayOrder
) {
}
