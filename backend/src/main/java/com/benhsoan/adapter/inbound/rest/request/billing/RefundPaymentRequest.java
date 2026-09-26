package com.benhsoan.adapter.inbound.rest.request.billing;

import jakarta.validation.constraints.NotBlank;

public record RefundPaymentRequest(
        @NotBlank String reason
) {
}
