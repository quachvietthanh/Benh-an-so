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

    @Test
    @DisplayName("adjustStock should update quantity and keep ACTIVE when positive")
    void adjustStockShouldUpdateQuantityAndKeepActive() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-ADJ-001",
                LocalDate.of(2027, 12, 31),
                100,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        batch.adjustStock(85, LocalDate.of(2026, 8, 7), UPDATED_AT);

        assertEquals(85, batch.getQuantity());
        assertEquals(BatchStatus.ACTIVE, batch.getStatus());
        assertEquals(UPDATED_AT, batch.getUpdatedAt());
    }

    @Test
    @DisplayName("adjustStock should set DEPLETED when actual quantity is zero")
    void adjustStockShouldSetDepletedWhenZero() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-ADJ-002",
                LocalDate.of(2027, 12, 31),
                50,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        batch.adjustStock(0, LocalDate.of(2026, 8, 7), UPDATED_AT);

        assertEquals(0, batch.getQuantity());
        assertEquals(BatchStatus.DEPLETED, batch.getStatus());
        assertEquals(UPDATED_AT, batch.getUpdatedAt());
    }

    @Test
    @DisplayName("adjustStock should reactivate DEPLETED batch when actual quantity becomes positive")
    void adjustStockShouldReactivateDepletedBatch() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-ADJ-003",
                LocalDate.of(2027, 12, 31),
                0,
                BatchStatus.DEPLETED,
                CREATED_AT,
                null
        );

        batch.adjustStock(20, LocalDate.of(2026, 8, 7), UPDATED_AT);

        assertEquals(20, batch.getQuantity());
        assertEquals(BatchStatus.ACTIVE, batch.getStatus());
        assertEquals(UPDATED_AT, batch.getUpdatedAt());
    }

    @Test
    @DisplayName("adjustStock should reject negative actual quantity")
    void adjustStockShouldRejectNegativeQuantity() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-ADJ-004",
                LocalDate.of(2027, 12, 31),
                50,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        assertThrows(ValidationException.class, () -> batch.adjustStock(-5, LocalDate.of(2026, 8, 7), UPDATED_AT));
    }

    @Test
    @DisplayName("adjustStock should reject adjustment when batch is already EXPIRED")
    void adjustStockShouldRejectWhenBatchIsExpired() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-ADJ-005",
                LocalDate.of(2026, 8, 1),
                0,
                BatchStatus.EXPIRED,
                CREATED_AT,
                null
        );

        assertThrows(com.benhsoan.domain.inventory.exception.BatchStateConflictException.class,
                () -> batch.adjustStock(10, LocalDate.of(2026, 8, 7), UPDATED_AT));
    }

    @Test
    @DisplayName("adjustStock should reject adjustment when batch expiry date is before today")
    void adjustStockShouldRejectWhenBatchIsExpiredByDate() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-ADJ-EXPIRED",
                LocalDate.of(2026, 8, 1),
                50,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        assertThrows(com.benhsoan.domain.inventory.exception.BatchStateConflictException.class,
                () -> batch.adjustStock(60, LocalDate.of(2026, 8, 7), UPDATED_AT));
    }

    @Test
    @DisplayName("adjustStock should reject adjustment when actual quantity equals current quantity")
    void adjustStockShouldRejectWhenQuantityUnchanged() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-ADJ-UNCHANGED",
                LocalDate.of(2027, 12, 31),
                50,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        assertThrows(ValidationException.class,
                () -> batch.adjustStock(50, LocalDate.of(2026, 8, 7), UPDATED_AT));
    }

    @Test
    @DisplayName("discardExpired should set quantity to zero and status to EXPIRED for expired batch")
    void discardExpiredShouldSetZeroAndStatusExpired() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-DISC-001",
                LocalDate.of(2026, 8, 1),
                50,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        batch.discardExpired(LocalDate.of(2026, 8, 7), UPDATED_AT);

        assertEquals(0, batch.getQuantity());
        assertEquals(BatchStatus.EXPIRED, batch.getStatus());
        assertEquals(UPDATED_AT, batch.getUpdatedAt());
    }

    @Test
    @DisplayName("discardExpired should reject when batch is not yet expired")
    void discardExpiredShouldRejectWhenNotExpired() {
        MedicineBatch batch = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-DISC-002",
                LocalDate.of(2026, 8, 10),
                50,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        assertThrows(com.benhsoan.domain.inventory.exception.BatchNotExpiredException.class,
                () -> batch.discardExpired(LocalDate.of(2026, 8, 7), UPDATED_AT));
    }

    @Test
    @DisplayName("discardExpired should reject when batch is already EXPIRED or quantity is zero")
    void discardExpiredShouldRejectWhenAlreadyDiscardedOrZero() {
        MedicineBatch alreadyExpired = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-DISC-003",
                LocalDate.of(2026, 8, 1),
                0,
                BatchStatus.EXPIRED,
                CREATED_AT,
                null
        );
        MedicineBatch zeroQuantity = MedicineBatch.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BATCH-DISC-004",
                LocalDate.of(2026, 8, 1),
                0,
                BatchStatus.ACTIVE,
                CREATED_AT,
                null
        );

        assertThrows(com.benhsoan.domain.inventory.exception.BatchAlreadyDiscardedException.class,
                () -> alreadyExpired.discardExpired(LocalDate.of(2026, 8, 7), UPDATED_AT));
        assertThrows(com.benhsoan.domain.inventory.exception.BatchAlreadyDiscardedException.class,
                () -> zeroQuantity.discardExpired(LocalDate.of(2026, 8, 7), UPDATED_AT));
    }
}
