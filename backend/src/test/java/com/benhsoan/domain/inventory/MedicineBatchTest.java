package com.benhsoan.domain.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.shared.exception.ValidationException;

@DisplayName("MedicineBatch Domain Tests")
class MedicineBatchTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-07T02:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-08-07T03:00:00Z");

    @Test
    @DisplayName("deductStock should reduce quantity and keep ACTIVE when stock remains")
    void deductStockShouldReduceQuantityAndKeepActive() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-001",
                LocalDate.of(2027, 12, 31),
                100,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        batch.deductStock(40, UPDATED_AT);

        assertEquals(60, batch.getQuantity());
        assertEquals(BatchStatus.ACTIVE, batch.getStatus());
        assertEquals(UPDATED_AT, batch.getUpdatedAt());
    }

    @Test
    @DisplayName("deductStock should mark batch DEPLETED when quantity reaches zero")
    void deductStockShouldMarkBatchDepleted() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-002",
                LocalDate.of(2027, 12, 31),
                25,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        batch.deductStock(25, UPDATED_AT);

        assertEquals(0, batch.getQuantity());
        assertEquals(BatchStatus.DEPLETED, batch.getStatus());
        assertEquals(UPDATED_AT, batch.getUpdatedAt());
    }

    @Test
    @DisplayName("deductStock should reject deduction greater than available quantity")
    void deductStockShouldRejectExcessiveDeduction() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-003",
                LocalDate.of(2027, 12, 31),
                10,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        assertThrows(ValidationException.class, () -> batch.deductStock(11, UPDATED_AT));
    }

    @Test
    @DisplayName("isEligibleForDispenseOn should accept active non-expired batches with stock")
    void shouldBeEligibleForDispense() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-004",
                LocalDate.of(2026, 8, 7),
                1,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        assertTrue(batch.isEligibleForDispenseOn(LocalDate.of(2026, 8, 7)));
    }

    @Test
    @DisplayName("isEligibleForDispenseOn should reject expired or depleted batches")
    void shouldRejectIneligibleBatchForDispense() {
        MedicineBatch expiredBatch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-005",
                LocalDate.of(2026, 8, 6),
                10,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );
        MedicineBatch depletedBatch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-006",
                LocalDate.of(2026, 8, 8),
                0,
                BatchStatus.DEPLETED,
                CREATED_AT,
                null
        );

        assertFalse(expiredBatch.isEligibleForDispenseOn(LocalDate.of(2026, 8, 7)));
        assertFalse(depletedBatch.isEligibleForDispenseOn(LocalDate.of(2026, 8, 7)));
    }

    @Test
    @DisplayName("expiry helpers should classify active batches with stock")
    void shouldClassifyExpiryAlerts() {
        MedicineBatch expiredBatch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-007",
                LocalDate.of(2026, 8, 6),
                10,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );
        MedicineBatch nearExpiryBatch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-008",
                LocalDate.of(2026, 8, 20),
                10,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        assertTrue(expiredBatch.isExpiredOn(LocalDate.of(2026, 8, 7)));
        assertFalse(expiredBatch.isNearExpiryOn(LocalDate.of(2026, 8, 7), 30));
        assertFalse(nearExpiryBatch.isExpiredOn(LocalDate.of(2026, 8, 7)));
        assertTrue(nearExpiryBatch.isNearExpiryOn(LocalDate.of(2026, 8, 7), 30));
    }
}
