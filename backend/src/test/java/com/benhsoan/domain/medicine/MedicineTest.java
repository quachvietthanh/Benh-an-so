package com.benhsoan.domain.medicine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.shared.exception.ValidationException;

class MedicineTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-21T10:00:00Z");

    private Medicine createMedicine(boolean controlled) {
        return Medicine.create(
                UUID.randomUUID(),
                "MED-001",
                "Paracetamol",
                "Acetaminophen",
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                20,
                controlled,
                CREATED_AT
        );
    }

    @Test
    void createOrdinaryMedicine_hasControlledFalseAndUpdatedAtNull() {
        Medicine medicine = createMedicine(false);

        assertFalse(medicine.isControlled());
        assertNotNull(medicine.getCreatedAt());
        assertEquals(CREATED_AT, medicine.getCreatedAt());
        assertNull(medicine.getUpdatedAt());
    }

    @Test
    void createControlledMedicine_hasControlledTrueAndUpdatedAtNull() {
        Medicine medicine = createMedicine(true);

        assertTrue(medicine.isControlled());
        assertNotNull(medicine.getCreatedAt());
        assertNull(medicine.getUpdatedAt());
    }

    @Test
    void markControlled_changesClassificationAndSetsUpdatedAt() {
        Medicine medicine = createMedicine(false);
        Instant updatedAt = CREATED_AT.plusSeconds(60);

        medicine.markControlled(true, updatedAt);

        assertTrue(medicine.isControlled());
        assertEquals(updatedAt, medicine.getUpdatedAt());
    }

    @Test
    void createInitializesActiveTrueAndZeroStock() {
        Medicine medicine = createMedicine(false);

        assertTrue(medicine.isActive());
        assertEquals(0, medicine.getStockQuantity());
        assertEquals(20, medicine.getMinStockThreshold());
    }

    @Test
    void createRejectsNegativeMinStockThreshold() {
        assertThrows(ValidationException.class, () -> Medicine.create(
                UUID.randomUUID(),
                "MED-001",
                "Paracetamol",
                "Acetaminophen",
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                -1,
                false,
                CREATED_AT
        ));
    }
}
