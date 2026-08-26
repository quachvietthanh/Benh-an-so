package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateMedicalRecordTemplateRequest(
        @NotNull UUID specialtyId,
        @NotBlank String name,
        Boolean makeDefault,
        @NotEmpty List<@Valid MedicalRecordTemplateSectionRequest> sections
) {
}
