package com.benhsoan.port.dto.command.inventory;

import java.util.UUID;

public record DiscardExpiredBatchCommand(
        UUID batchId,
        String reason
) {
}
