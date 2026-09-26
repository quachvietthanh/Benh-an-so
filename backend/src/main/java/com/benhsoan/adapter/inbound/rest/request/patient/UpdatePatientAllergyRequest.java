package com.benhsoan.adapter.inbound.rest.request.patient;

import com.benhsoan.domain.patient.enums.AllergySeverity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePatientAllergyRequest(
        @NotBlank(message = "Tên hoạt chất hoặc nhóm thuốc không được để trống")
        @Size(max = 255, message = "Tên hoạt chất hoặc nhóm thuốc không quá 255 ký tự")
        String allergenName,

        @NotNull(message = "Mức độ phản ứng không được để trống")
        AllergySeverity severity,

        @Size(max = 255, message = "Biểu hiện phản ứng không quá 255 ký tự")
        String reaction,

        String notes,

        String changeReason
) {
}
