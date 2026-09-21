package com.benhsoan.port.dto.query.inventory;

import java.time.LocalDate;
import java.util.UUID;

public record GetInventoryInOutStockReportQuery(
        LocalDate from,
        LocalDate to,
        UUID medicineId,
        String keyword
) {
}
