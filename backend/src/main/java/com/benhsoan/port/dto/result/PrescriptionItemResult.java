package com.benhsoan.port.dto.result;

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

        String instructions,

        Instant createdAt,

        Instant updatedAt

) {
}
