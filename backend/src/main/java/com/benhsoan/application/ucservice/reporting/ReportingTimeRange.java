package com.benhsoan.application.ucservice.reporting;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

record ReportingTimeRange(
        LocalDate from,
        LocalDate to,
        Instant fromInclusive,
        Instant toExclusive
) {

    static ReportingTimeRange of(LocalDate from, LocalDate to) {
        return new ReportingTimeRange(
                from,
                to,
                from.atStartOfDay().toInstant(ZoneOffset.UTC),
                to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        );
    }
}
