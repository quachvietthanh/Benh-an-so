package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OperationalSummaryResponse(
        LocalDate from,
        LocalDate to,
        long visitCount,
        BigDecimal revenue,
        String currency
) {
}
