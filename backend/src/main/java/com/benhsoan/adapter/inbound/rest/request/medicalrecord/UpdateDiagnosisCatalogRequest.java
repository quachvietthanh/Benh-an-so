package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDiagnosisCatalogRequest(
        @NotBlank(message = "Diagnosis name is required.")
        @Size(max = 150, message = "Diagnosis name must not exceed 150 characters.")
        String name,

        @Size(max = 50, message = "Diagnosis abbreviation must not exceed 50 characters.")
        String abbreviation,

        @NotBlank(message = "Disease group is required.")
        @Size(max = 100, message = "Disease group must not exceed 100 characters.")
        String diseaseGroup,

        String description
) {
}
