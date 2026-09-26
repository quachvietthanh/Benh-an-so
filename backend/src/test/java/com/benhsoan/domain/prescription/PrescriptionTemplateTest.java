package com.benhsoan.domain.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.shared.exception.ValidationException;

class PrescriptionTemplateTest {

    private static final Instant NOW = Instant.parse("2026-09-30T01:00:00Z");
    private static final UUID DIAGNOSIS_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();

    @Test
    void createRequiresAtLeastOneItem() {
        assertThrows(ValidationException.class, () -> PrescriptionTemplate.create(
                UUID.randomUUID(), DIAGNOSIS_ID, DOCTOR_ID, NOW, List.of()));
    }

    @Test
    void createRejectsDuplicateMedicine() {
        UUID medicineId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        List<PrescriptionTemplateItem> items = List.of(
                item(templateId, medicineId, 0),
                item(templateId, medicineId, 1)
        );

        assertThrows(ValidationException.class, () -> PrescriptionTemplate.create(
                templateId, DIAGNOSIS_ID, DOCTOR_ID, NOW, items));
    }

    @Test
    void itemsAreSortedBySortOrder() {
        UUID templateId = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PrescriptionTemplate template = PrescriptionTemplate.restore(
                templateId,
                DIAGNOSIS_ID,
                DOCTOR_ID,
                NOW,
                List.of(item(templateId, second, 5), item(templateId, first, 1))
        );

        assertEquals(first, template.getItems().get(0).getMedicineId());
        assertEquals(second, template.getItems().get(1).getMedicineId());
    }

    private PrescriptionTemplateItem item(UUID templateId, UUID medicineId, int sortOrder) {
        return PrescriptionTemplateItem.create(
                templateId,
                medicineId,
                "1 viên",
                2,
                AdministrationRoute.ORAL,
                7,
                14,
                null,
                sortOrder
        );
    }
}
