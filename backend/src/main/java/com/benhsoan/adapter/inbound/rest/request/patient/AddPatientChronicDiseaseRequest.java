package com.benhsoan.adapter.inbound.rest.request.patient;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddPatientChronicDiseaseRequest(
        @NotNull(message = "Mã bệnh (chẩn đoán) không được để trống")
        UUID diagnosisCatalogId,

        @Min(value = 1900, message = "Năm phát hiện bệnh không hợp lệ")
        Integer yearDetected,

        String notes,

        UUID visitId
) {
}
