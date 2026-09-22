package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CurrentShiftSummaryResult(
        UUID cashierId,
        String cashierName,
        Instant startTime,
        Instant endTime,
        int totalTransactions,
        BigDecimal systemCashAmount,
        BigDecimal systemTransferAmount,
        BigDecimal systemCardAmount,
        BigDecimal systemOtherAmount,
        BigDecimal totalSystemAmount,
        List<UUID> unsettledPaymentIds
) {
}
