package com.benhsoan.port.dto.command.inventory;

import java.util.Map;
import java.util.UUID;

public record ApproveProcurementPlanCommand(
        UUID planId,
        String note,
        Map<UUID, Integer> approvedQuantities
) {
}
