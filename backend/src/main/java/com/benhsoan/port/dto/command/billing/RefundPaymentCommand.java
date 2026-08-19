package com.benhsoan.port.dto.command.billing;

import java.util.UUID;

public record RefundPaymentCommand(
        UUID paymentId,
        String reason
) {
}
