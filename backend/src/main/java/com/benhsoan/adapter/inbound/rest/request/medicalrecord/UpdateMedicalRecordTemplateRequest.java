package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UpdateMedicalRecordTemplateRequest(
        @NotBlank String name,
        @NotEmpty List<@Valid MedicalRecordTemplateSectionRequest> sections,
        String changeNote
) {
}
