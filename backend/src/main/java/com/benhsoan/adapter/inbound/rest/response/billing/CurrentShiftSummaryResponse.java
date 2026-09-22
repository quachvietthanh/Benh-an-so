package com.benhsoan.adapter.inbound.rest.response.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CurrentShiftSummaryResponse(
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
