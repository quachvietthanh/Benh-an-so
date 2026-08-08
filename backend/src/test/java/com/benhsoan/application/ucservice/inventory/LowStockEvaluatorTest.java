package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;

class LowStockEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-08-08T00:00:00Z");

    private final LowStockEvaluator evaluator = new LowStockEvaluator();

    @Test
    void calculatesEligibleStockUsingOnlyDispensableBatches() {
        UUID medicineId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 8, 8);

        List<MedicineBatch> batches = List.of(
                MedicineBatch.restore(UUID.randomUUID(), medicineId, "B1", LocalDate.of(2026, 8, 8), 10, BatchStatus.ACTIVE, NOW, null),
                MedicineBatch.restore(UUID.randomUUID(), medicineId, "B2", LocalDate.of(2026, 8, 20), 20, BatchStatus.ACTIVE, NOW, null),
                MedicineBatch.restore(UUID.randomUUID(), medicineId, "B3", LocalDate.of(2026, 8, 7), 30, BatchStatus.ACTIVE, NOW, null),
                MedicineBatch.restore(UUID.randomUUID(), medicineId, "B4", LocalDate.of(2026, 8, 20), 40, BatchStatus.DEPLETED, NOW, null)
        );

        assertEquals(30, evaluator.calculateEligibleStockQuantity(batches, today));
    }

    @Test
    void flagsMedicineAsLowStockWhenEligibleQuantityIsBelowThreshold() {
        Medicine medicine = medicine(50);

        assertTrue(evaluator.isLowStock(medicine, 49));
        assertFalse(evaluator.isLowStock(medicine, 50));
        assertEquals(1, evaluator.calculateShortageQuantity(medicine, 49));
    }

    private Medicine medicine(int minStockThreshold) {
        return Medicine.restore(
                UUID.randomUUID(),
                "MED-001",
                "Paracetamol",
                "Paracetamol",
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                true,
                NOW.minusSeconds(3600),
                null,
                120,
                minStockThreshold
        );
    }
}
