package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiptItemResponse(
        UUID id,
        UUID medicineId,
        String batchNumber,
        LocalDate expiryDate,
        int quantity,
        BigDecimal importPrice,
        BigDecimal totalValue
) {
}
