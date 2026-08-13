package com.benhsoan.port.outbound.repository.reporting;

import java.time.LocalDate;

public record DailyVisitSummary(
        LocalDate date,
        long visitCount
) {
}
