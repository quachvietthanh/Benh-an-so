package com.benhsoan.application.ucservice.reporting;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

record ReportingTimeRange(
        LocalDate from,
        LocalDate to,
        Instant fromInclusive,
        Instant toExclusive
) {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    static ReportingTimeRange of(LocalDate from, LocalDate to) {
        return new ReportingTimeRange(
                from,
                to,
                from.atStartOfDay(CLINIC_ZONE).toInstant(),
                to.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant()
        );
    }
}
