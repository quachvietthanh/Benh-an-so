package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.InvoiceLineType;

public record InvoiceLineResult(
        UUID id,
        UUID invoiceId,
        InvoiceLineType lineType,
        String itemName,
        UUID referenceId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        Instant createdAt
) {
}
