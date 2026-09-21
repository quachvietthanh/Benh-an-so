package com.benhsoan.domain.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.shared.exception.ValidationException;

class MedicationReturnDomainTest {

    private static final Instant NOW = Instant.parse("2026-08-20T03:00:00Z");

    @Test
    void recordReturnReducesRemainingReturnableQuantity() {
        PrescriptionDispenseItem dispenseItem = PrescriptionDispenseItem.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 12, UUID.randomUUID(), NOW);

        dispenseItem.recordReturn(5);

        assertEquals(5, dispenseItem.getReturnedQuantity());
        assertEquals(7, dispenseItem.getRemainingReturnableQuantity());
    }

    @Test
    void recordReturnRejectsExceedingReturnableQuantity() {
        PrescriptionDispenseItem dispenseItem = PrescriptionDispenseItem.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 12, UUID.randomUUID(), NOW);

        assertThrows(ValidationException.class, () -> dispenseItem.recordReturn(13));
    }

    @Test
    void recordReturnRejectsNonPositiveQuantity() {
        PrescriptionDispenseItem dispenseItem = PrescriptionDispenseItem.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 12, UUID.randomUUID(), NOW);

        assertThrows(ValidationException.class, () -> dispenseItem.recordReturn(0));
    }

    @Test
    void prescriptionItemRecordReturnDecrementsDispensedQuantity() {
        PrescriptionItem item = PrescriptionItem.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Paracetamol", "Paracetamol", "500 mg", "vien", "1 vien",
                3, AdministrationRoute.ORAL, 3, 20, "After meal", NOW);

        item.recordDispense(12);
        item.recordReturn(5);

        assertEquals(7, item.getDispensedQuantity());
        assertEquals(13, item.getRemainingQuantity());
    }

    @Test
    void prescriptionItemRecordReturnRejectsExceedingDispensedQuantity() {
        PrescriptionItem item = PrescriptionItem.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Paracetamol", "Paracetamol", "500 mg", "vien", "1 vien",
                3, AdministrationRoute.ORAL, 3, 20, "After meal", NOW);

        item.recordDispense(12);

        assertThrows(ValidationException.class, () -> item.recordReturn(13));
    }
}
