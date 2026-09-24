package com.benhsoan.port.dto.command.inventory;

import java.util.List;
import java.util.UUID;

public record UpdateProcurementPlanCommand(
        UUID planId,
        String note,
        List<CreateProcurementPlanItemCommand> items
) {
}
