package com.benhsoan.adapter.inbound.rest.request.clinical;

import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateClinicalServiceRequest(
        @NotBlank(message = "Service name is required.")
        @Size(max = 150, message = "Service name must not exceed 150 characters.")
        String serviceName,

        @NotNull(message = "Service type is required.")
        ClinicalServiceType serviceType,

        @NotNull(message = "Result data type is required.")
        ClinicalResultDataType resultDataType,

        @Size(max = 50, message = "Unit must not exceed 50 characters.")
        String unit,

        @Size(max = 255, message = "Reference range must not exceed 255 characters.")
        String referenceRange,

        String description
) {
}
