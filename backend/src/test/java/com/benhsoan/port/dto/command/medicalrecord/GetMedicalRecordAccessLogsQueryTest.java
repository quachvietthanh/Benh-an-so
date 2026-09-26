package com.benhsoan.port.dto.command.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class GetMedicalRecordAccessLogsQueryTest {

    @Test
    @DisplayName("accepts multi-filter search without requiring patientId or medicalRecordId")
    void acceptsMultiFilterSearch() {
        assertDoesNotThrow(() -> new GetMedicalRecordAccessLogsQuery(
                UUID.randomUUID(),
                null,
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-12T23:59:59Z"),
                0,
                20
        ));
    }

    @Test
    @DisplayName("accepts empty business filters when paging is valid")
    void acceptsEmptyBusinessFilters() {
        assertDoesNotThrow(() -> new GetMedicalRecordAccessLogsQuery(
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        ));
    }

    @Test
    @DisplayName("rejects invalid time range")
    void rejectsInvalidTimeRange() {
        assertThrows(ValidationException.class, () -> new GetMedicalRecordAccessLogsQuery(
                null,
                UUID.randomUUID(),
                null,
                null,
                Instant.parse("2026-08-12T23:59:59Z"),
                Instant.parse("2026-08-01T00:00:00Z"),
                0,
                20
        ));
    }

    @Test
    @DisplayName("rejects invalid page size")
    void rejectsInvalidPageSize() {
        assertThrows(ValidationException.class, () -> new GetMedicalRecordAccessLogsQuery(
                null,
                null,
                UUID.randomUUID(),
                null,
                null,
                null,
                0,
                101
        ));
    }

    @Test
    @DisplayName("rejects negative page")
    void rejectsNegativePage() {
        assertThrows(ValidationException.class, () -> new GetMedicalRecordAccessLogsQuery(
                null,
                null,
                null,
                null,
                null,
                null,
                -1,
                20
        ));
    }
}
