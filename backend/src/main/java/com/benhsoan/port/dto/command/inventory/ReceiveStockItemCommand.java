package com.benhsoan.port.dto.command.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiveStockItemCommand(
        UUID medicineId,
        String batchNumber,
        LocalDate expiryDate,
        int quantity,
        BigDecimal importPrice
) {
}
