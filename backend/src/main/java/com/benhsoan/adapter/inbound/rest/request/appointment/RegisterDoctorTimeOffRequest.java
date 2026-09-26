package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDoctorTimeOffRequest(
        @NotNull(message = "Thời gian bắt đầu nghỉ không được để trống.")
        Instant startTime,

        @NotNull(message = "Thời gian kết thúc nghỉ không được để trống.")
        Instant endTime,

        @NotBlank(message = "Lý do nghỉ không được để trống.")
        @Size(max = 500, message = "Lý do nghỉ không được vượt quá 500 ký tự.")
        String reason
) {
}
