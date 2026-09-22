package com.benhsoan.port.dto.command.billing;

import java.util.UUID;

import lombok.Builder;

@Builder
public record RejectDiscountRequestCommand(
        UUID discountRequestId,
        String rejectionReason
) {
}
