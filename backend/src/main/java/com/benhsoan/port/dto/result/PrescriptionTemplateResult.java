package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

public record PrescriptionTemplateResult(
        UUID id,
        UUID diagnosisCatalogId,
        String diagnosisCode,
        String diagnosisName,
        UUID createdBy,
        Instant createdAt,
        List<Item> items
) {
    public record Item(
            UUID id,
            UUID medicineId,
            String medicineCode,
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String dosage,
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions,
            int sortOrder
    ) {
    }
}
