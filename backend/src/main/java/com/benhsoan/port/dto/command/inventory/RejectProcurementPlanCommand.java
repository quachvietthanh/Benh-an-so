package com.benhsoan.port.dto.command.inventory;

import java.util.UUID;

public record RejectProcurementPlanCommand(
        UUID planId,
        String reason
) {
}
