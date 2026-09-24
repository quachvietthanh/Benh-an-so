package com.benhsoan.port.dto.command.inventory;

import java.time.LocalDate;
import java.util.List;

public record CreateProcurementPlanCommand(
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        String note,
        boolean submitImmediately,
        List<CreateProcurementPlanItemCommand> items
) {
}
