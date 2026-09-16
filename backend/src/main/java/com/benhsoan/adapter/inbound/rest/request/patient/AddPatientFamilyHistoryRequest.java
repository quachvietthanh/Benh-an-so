package com.benhsoan.adapter.inbound.rest.request.patient;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddPatientFamilyHistoryRequest(
        @NotBlank(message = "Mối quan hệ gia đình không được để trống")
        @Size(max = 100, message = "Mối quan hệ gia đình không quá 100 ký tự")
        String relationship,

        @NotNull(message = "Mã bệnh (chẩn đoán) không được để trống")
        UUID diagnosisCatalogId,

        String notes,

        UUID visitId
) {
}
