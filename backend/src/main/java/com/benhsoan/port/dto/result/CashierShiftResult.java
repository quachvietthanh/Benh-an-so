package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.CashierShiftStatus;

public record CashierShiftResult(
        UUID id,
        String shiftCode,
        UUID cashierId,
        String cashierName,
        Instant startTime,
        Instant endTime,
        int totalTransactions,
        BigDecimal totalSystemAmount,
        BigDecimal systemCashAmount,
        BigDecimal systemTransferAmount,
        BigDecimal systemCardAmount,
        BigDecimal systemOtherAmount,
        BigDecimal actualCashAmount,
        BigDecimal differenceAmount,
        CashierShiftStatus status,
        String notes,
        UUID confirmedBy,
        String confirmedByName,
        Instant confirmedAt,
        String confirmationNotes,
        Instant createdAt
) {
}
