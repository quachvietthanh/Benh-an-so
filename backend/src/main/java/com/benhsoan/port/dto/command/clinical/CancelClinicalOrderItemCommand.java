package com.benhsoan.port.dto.command.clinical;

import java.util.UUID;

public record CancelClinicalOrderItemCommand(
        UUID orderItemId,
        String cancelReason
) {
}
