package com.benhsoan.domain.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.shared.exception.ValidationException;

class ClinicalResultHistoryTest {

    private final Instant now = Instant.parse("2026-08-20T02:00:00Z");

    @Test
    void capturesCompleteOldAndNewResultSnapshots() {
        UUID resultId = UUID.randomUUID();
        ClinicalResult oldResult = result(resultId, BigDecimal.ONE, "Initial conclusion");
        ClinicalResult newResult = result(resultId, BigDecimal.TEN, "Corrected conclusion");

        ClinicalResultHistory history = ClinicalResultHistory.create(
                oldResult, newResult, "Corrected value", UUID.randomUUID(), now
        );

        assertEquals(BigDecimal.ONE, history.getOldNumericValue());
        assertEquals(BigDecimal.TEN, history.getNewNumericValue());
        assertEquals("Initial conclusion", history.getOldConclusion());
        assertEquals("Corrected conclusion", history.getNewConclusion());
        assertEquals(resultId, history.getClinicalResultId());
    }

    @Test
    void rejectsSnapshotsFromDifferentResults() {
        assertThrows(ValidationException.class, () -> ClinicalResultHistory.create(
                result(UUID.randomUUID(), BigDecimal.ONE, "First"),
                result(UUID.randomUUID(), BigDecimal.TEN, "Second"),
                "Invalid comparison", UUID.randomUUID(), now
        ));
    }

    private ClinicalResult result(UUID id, BigDecimal value, String conclusion) {
        return ClinicalResult.restore(
                id, UUID.randomUUID(), UUID.randomUUID(), ClinicalResultType.NUMBER,
                value, null, "mmol/L", "1-2", ClinicalResultAbnormalFlag.NORMAL,
                conclusion, com.benhsoan.domain.clinical.enums.ClinicalResultStatus.DRAFT,
                UUID.randomUUID(), now, null, null
        );
    }
}
