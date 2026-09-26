package com.benhsoan.port.dto.command.patient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class GetPatientMedicalHistoryQueryTest {

    @Test
    void acceptsValidDateRangeAndPaging() {
        assertDoesNotThrow(() -> new GetPatientMedicalHistoryQuery(
                UUID.randomUUID(),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-31T23:59:59Z"),
                0,
                20
        ));
    }

    @Test
    void rejectsInvalidDateRange() {
        assertThrows(ValidationException.class, () -> new GetPatientMedicalHistoryQuery(
                UUID.randomUUID(),
                Instant.parse("2026-08-31T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"),
                0,
                20
        ));
    }

    @Test
    void rejectsPageSizeOutsideSupportedRange() {
        assertThrows(ValidationException.class, () -> new GetPatientMedicalHistoryQuery(
                UUID.randomUUID(), null, null, 0, 101
        ));
    }
}
