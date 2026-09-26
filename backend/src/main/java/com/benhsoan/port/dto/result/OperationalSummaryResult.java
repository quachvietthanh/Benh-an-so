package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OperationalSummaryResult(
        LocalDate from,
        LocalDate to,
        long visitCount,
        BigDecimal revenue,
        String currency
) {
}
