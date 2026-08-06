package com.benhsoan.adapter.inbound.rest.response.medicine;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;

public record MedicineResponse(
        UUID id,
        String medicineCode,
        String medicineName,
        String activeIngredient,
        String strength,
        DosageForm dosageForm,
        String unit,
        AdministrationRoute defaultRoute,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
