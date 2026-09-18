package com.benhsoan.adapter.inbound.rest.request.visit;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HandoverPatientRequest(
        @NotNull(message = "Bác sĩ tiếp nhận không được để trống")
        UUID targetDoctorId,

        @NotBlank(message = "Lý do bàn giao không được để trống")
        @Size(max = 500, message = "Lý do bàn giao không được vượt quá 500 ký tự")
        String reason
) {
}
