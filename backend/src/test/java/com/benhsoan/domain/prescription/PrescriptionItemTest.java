package com.benhsoan.domain.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.shared.exception.ValidationException;

class PrescriptionItemTest {

    private static final Instant NOW = Instant.parse("2026-08-07T02:00:00Z");
    private static final UUID PRESCRIPTION_ID = UUID.randomUUID();

    @Test
    void recordsCumulativeDispense() {
        PrescriptionItem item = item(20, 0);

        item.recordDispense(12);

        assertEquals(12, item.getDispensedQuantity());
        assertEquals(8, item.getRemainingQuantity());
        assertFalse(item.isFullyDispensed());
    }

    @Test
    void completesWhenCumulativeDispenseEqualsPrescribed() {
        PrescriptionItem item = item(20, 12);

        item.recordDispense(8);

        assertEquals(20, item.getDispensedQuantity());
        assertEquals(0, item.getRemainingQuantity());
        assertTrue(item.isFullyDispensed());
    }

    @Test
    void rejectsNonPositiveDispense() {
        PrescriptionItem item = item(20, 0);

        assertThrows(ValidationException.class, () -> item.recordDispense(0));
        assertThrows(ValidationException.class, () -> item.recordDispense(-1));
    }

    @Test
    void rejectsDispenseExceedingPrescribed() {
        PrescriptionItem item = item(20, 15);

        assertThrows(ValidationException.class, () -> item.recordDispense(6));
        assertEquals(15, item.getDispensedQuantity());
    }

    @Test
    void restoresWithExistingDispensedQuantity() {
        PrescriptionItem item = item(20, 12);

        assertEquals(12, item.getDispensedQuantity());
        assertEquals(8, item.getRemainingQuantity());
    }

    private PrescriptionItem item(int prescribed, int dispensed) {
        return PrescriptionItem.restore(
                UUID.randomUUID(),
                PRESCRIPTION_ID,
                UUID.randomUUID(),
                "Paracetamol",
                "Paracetamol",
                "500 mg",
                "vien",
                "1 vien",
                2,
                AdministrationRoute.ORAL,
                5,
                prescribed,
                dispensed,
                null,
                NOW.minusSeconds(600),
                null);
    }
}
