package com.benhsoan.domain.prescription;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.shared.exception.ValidationException;

class PrescriptionTemplateItemTest {

    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final UUID MEDICINE_ID = UUID.randomUUID();

    @Test
    void validItemIsAccepted() {
        assertDoesNotThrow(() -> item("1 viên", 2, AdministrationRoute.ORAL, 7, 14, 0));
    }

    @Test
    void blankDosageIsRejected() {
        assertThrows(ValidationException.class,
                () -> item("  ", 2, AdministrationRoute.ORAL, 7, 14, 0));
    }

    @Test
    void nullDosageIsRejected() {
        assertThrows(ValidationException.class,
                () -> item(null, 2, AdministrationRoute.ORAL, 7, 14, 0));
    }

    @Test
    void nonPositiveFrequencyIsRejected() {
        assertThrows(ValidationException.class,
                () -> item("1 viên", 0, AdministrationRoute.ORAL, 7, 14, 0));
        assertThrows(ValidationException.class,
                () -> item("1 viên", null, AdministrationRoute.ORAL, 7, 14, 0));
    }

    @Test
    void nonPositiveDurationDaysIsRejected() {
        assertThrows(ValidationException.class,
                () -> item("1 viên", 2, AdministrationRoute.ORAL, 0, 14, 0));
        assertThrows(ValidationException.class,
                () -> item("1 viên", 2, AdministrationRoute.ORAL, null, 14, 0));
    }

    @Test
    void nonPositiveQuantityIsRejected() {
        assertThrows(ValidationException.class,
                () -> item("1 viên", 2, AdministrationRoute.ORAL, 7, 0, 0));
    }

    @Test
    void negativeSortOrderIsRejected() {
        assertThrows(ValidationException.class,
                () -> item("1 viên", 2, AdministrationRoute.ORAL, 7, 14, -1));
    }

    @Test
    void nullRouteIsRejected() {
        assertThrows(ValidationException.class,
                () -> item("1 viên", 2, null, 7, 14, 0));
    }

    @Test
    void sortOrderZeroIsAccepted() {
        PrescriptionTemplateItem item = item("1 viên", 2, AdministrationRoute.ORAL, 7, 14, 0);
        assertEquals(0, item.getSortOrder());
    }

    @Test
    void dosageIsTrimmed() {
        PrescriptionTemplateItem item = item("  1 viên  ", 2, AdministrationRoute.ORAL, 7, 14, 0);
        assertEquals("1 viên", item.getDosage());
    }

    private PrescriptionTemplateItem item(
            String dosage,
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            int sortOrder
    ) {
        return PrescriptionTemplateItem.create(
                TEMPLATE_ID, MEDICINE_ID, dosage, frequency, route, durationDays, quantity, null, sortOrder);
    }
}
