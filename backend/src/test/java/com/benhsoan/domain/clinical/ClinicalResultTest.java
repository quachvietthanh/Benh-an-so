package com.benhsoan.domain.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.clinical.exception.ClinicalResultAlreadyFinalizedException;
import com.benhsoan.domain.clinical.exception.ClinicalResultInvalidStatusException;

class ClinicalResultTest {

    private final Instant now = Instant.parse("2026-08-20T02:00:00Z");

    @Test
    void finalizesThenCorrectsAResult() {
        ClinicalResult result = createDraft();

        result.finalizeResult(UUID.randomUUID(), now);
        result.markCorrected(UUID.randomUUID(), now.plusSeconds(1));

        assertEquals(ClinicalResultStatus.CORRECTED, result.getStatus());
    }

    @Test
    void rejectsUpdateOfFinalResult() {
        ClinicalResult result = createDraft();
        result.finalizeResult(UUID.randomUUID(), now);

        assertThrows(ClinicalResultAlreadyFinalizedException.class,
                () -> result.updateResult(BigDecimal.TEN, null, "mmol/L", null,
                        ClinicalResultAbnormalFlag.NORMAL, null, UUID.randomUUID(), now));
    }

    @Test
    void rejectsCorrectionBeforeFinalization() {
        assertThrows(ClinicalResultInvalidStatusException.class,
                () -> createDraft().markCorrected(UUID.randomUUID(), now));
    }

    @Test
    void mapsFileDataTypeToFileResultType() {
        assertEquals(ClinicalResultType.FILE,
                ClinicalResultType.from(com.benhsoan.domain.clinical.enums.ClinicalResultDataType.FILE));
        assertTrue(ClinicalResultType.FILE.requiresAttachment());
    }

    private ClinicalResult createDraft() {
        return ClinicalResult.create(
                UUID.randomUUID(), UUID.randomUUID(), ClinicalResultType.NUMBER,
                BigDecimal.ONE, null, "mmol/L", "1-2", ClinicalResultAbnormalFlag.NORMAL,
                "Within range", UUID.randomUUID(), now
        );
    }
}
