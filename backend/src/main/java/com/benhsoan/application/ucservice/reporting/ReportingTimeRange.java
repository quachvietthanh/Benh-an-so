package com.benhsoan.application.ucservice.reporting;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.benhsoan.domain.shared.exception.ValidationException;

record ReportingTimeRange(
        LocalDate from,
        LocalDate to,
        Instant fromInclusive,
        Instant toExclusive
) {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    static ReportingTimeRange of(LocalDate from, LocalDate to) {
        if (from == null) {
            throw new ValidationException("from is required.");
        }
        if (to == null) {
            throw new ValidationException("to is required.");
        }
        if (from.isAfter(to)) {
            throw new ValidationException("from must be before or equal to to.");
        }
        return new ReportingTimeRange(
                from,
                to,
                from.atStartOfDay(CLINIC_ZONE).toInstant(),
                to.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant()
        );
    }
}
