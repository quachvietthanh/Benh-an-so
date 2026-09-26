package com.benhsoan.adapter.inbound.rest.request.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ResetPasswordRequest(

        String temporaryPassword,

        @Min(value = 1, message = "Thời gian hiệu lực tối thiểu là 1 giờ")
        @Max(value = 720, message = "Thời gian hiệu lực tối đa là 720 giờ (30 ngày)")
        Integer expiresInHours

) {
    public ResetPasswordRequest(String temporaryPassword) {
        this(temporaryPassword, null);
    }
}

