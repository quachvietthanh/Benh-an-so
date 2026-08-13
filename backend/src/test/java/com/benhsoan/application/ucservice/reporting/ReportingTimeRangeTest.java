package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ReportingTimeRangeTest {

    @Test
    void normalizesRangeToStartOfDayAndNextDayExclusive() {
        ReportingTimeRange range = ReportingTimeRange.of(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3)
        );

        assertEquals(Instant.parse("2026-07-31T17:00:00Z"), range.fromInclusive());
        assertEquals(Instant.parse("2026-08-03T17:00:00Z"), range.toExclusive());
    }
}
