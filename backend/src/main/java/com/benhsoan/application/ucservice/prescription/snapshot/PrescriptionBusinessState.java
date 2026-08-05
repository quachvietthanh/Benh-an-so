package com.benhsoan.application.ucservice.prescription.snapshot;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

public record PrescriptionBusinessState(
        String note,
        List<Item> items
) {

    public record Item(
            UUID medicineId,
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String dosage,
            String frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions
    ) {
    }
}
