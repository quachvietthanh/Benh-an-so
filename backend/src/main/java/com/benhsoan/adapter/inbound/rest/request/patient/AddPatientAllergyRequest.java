package com.benhsoan.adapter.inbound.rest.request.patient;

import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddPatientAllergyRequest(
        @Size(max = 50, message = "Loại dị ứng không quá 50 ký tự")
        String allergenType,

        @NotBlank(message = "Tên hoạt chất hoặc nhóm thuốc không được để trống")
        @Size(max = 255, message = "Tên hoạt chất hoặc nhóm thuốc không quá 255 ký tự")
        String allergenName,

        @NotNull(message = "Mức độ phản ứng không được để trống")
        AllergySeverity severity,

        @Size(max = 255, message = "Biểu hiện phản ứng không quá 255 ký tự")
        String reaction,

        String notes,

        UUID visitId
) {
}
