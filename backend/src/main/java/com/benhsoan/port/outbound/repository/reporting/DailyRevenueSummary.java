package com.benhsoan.port.outbound.repository.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueSummary(
        LocalDate date,
        BigDecimal revenue
) {
}
