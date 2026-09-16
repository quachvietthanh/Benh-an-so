package com.benhsoan.adapter.inbound.rest.request.patient;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MergePatientsRequest(

        @NotNull(message = "ID hồ sơ nguồn không được để trống")
        UUID sourcePatientId,

        @NotNull(message = "ID hồ sơ đích không được để trống")
        UUID targetPatientId,

        @Size(max = 500, message = "Lý do gộp không được vượt quá 500 ký tự")
        String reason

) {}
