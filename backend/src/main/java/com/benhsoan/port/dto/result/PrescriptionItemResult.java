package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

public record PrescriptionItemResult(

        UUID id,

        UUID prescriptionId,

        UUID medicineId,

        String medicineName,

        String activeIngredient,

        String strength,

        String unit,

        String dosage,

        Integer frequency,

        AdministrationRoute route,

        Integer durationDays,

        int quantity,

        int dispensedQuantity,

        int remainingQuantity,

        String instructions,

        BigDecimal singleDoseQuantity,

        Instant createdAt,

        Instant updatedAt

) {
    public PrescriptionItemResult(
            UUID id,
            UUID prescriptionId,
            UUID medicineId,
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String dosage,
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            int dispensedQuantity,
            int remainingQuantity,
            String instructions,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, prescriptionId, medicineId, medicineName, activeIngredient, strength, unit, dosage, frequency,
                route, durationDays, quantity, dispensedQuantity, remainingQuantity, instructions, null, createdAt,
                updatedAt);
    }
}
