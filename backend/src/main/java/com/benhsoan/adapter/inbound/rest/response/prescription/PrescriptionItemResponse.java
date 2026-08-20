package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

import lombok.Builder;

@Builder
public record PrescriptionItemResponse(

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
