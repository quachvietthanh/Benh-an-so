package com.benhsoan.adapter.inbound.rest.request.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelWaitlistRequest(
        @NotBlank(message = "Lý do hủy không được để trống.")
        @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự.")
        String reason
) {
}
