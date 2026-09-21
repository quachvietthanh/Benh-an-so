package com.benhsoan.application.ucservice.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

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

    @Test
    void rejectsNullFrom() {
        assertThrows(ValidationException.class,
                () -> ReportingTimeRange.of(null, LocalDate.of(2026, 8, 3)));
    }

    @Test
    void rejectsNullTo() {
        assertThrows(ValidationException.class,
                () -> ReportingTimeRange.of(LocalDate.of(2026, 8, 1), null));
    }

    @Test
    void rejectsFromAfterTo() {
        assertThrows(ValidationException.class,
                () -> ReportingTimeRange.of(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 1)));
    }

    @Test
    void acceptsEqualFromAndTo() {
        ReportingTimeRange range = ReportingTimeRange.of(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 1)
        );

        assertEquals(Instant.parse("2026-07-31T17:00:00Z"), range.fromInclusive());
        assertEquals(Instant.parse("2026-08-01T17:00:00Z"), range.toExclusive());
    }
}
