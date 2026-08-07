package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryReceiptItemResult(
        UUID id,
        UUID medicineId,
        String batchNumber,
        LocalDate expiryDate,
        int quantity,
        BigDecimal importPrice,
        BigDecimal totalValue
) {
}
