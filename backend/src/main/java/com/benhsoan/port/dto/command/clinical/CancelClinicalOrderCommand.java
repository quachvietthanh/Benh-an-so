package com.benhsoan.port.dto.command.clinical;

import java.util.UUID;

public record CancelClinicalOrderCommand(
        UUID orderId,
        String cancelReason
) {
}
