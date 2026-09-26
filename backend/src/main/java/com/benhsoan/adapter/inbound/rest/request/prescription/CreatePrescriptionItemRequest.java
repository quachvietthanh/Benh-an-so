package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.math.BigDecimal;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePrescriptionItemRequest(

        @NotNull
        UUID medicineId,

        @NotBlank
        @Size(max = 100)
        String dosage,

        @NotNull
        @Positive
        Integer frequency,

        @NotNull
        AdministrationRoute route,

        @NotNull
        @Positive
        Integer durationDays,

        @Positive
        int quantity,

        String instructions,

        @Positive
        BigDecimal singleDoseQuantity

) {
    public CreatePrescriptionItemRequest(
            UUID medicineId,
            String dosage,
            Integer frequency,
            AdministrationRoute route,
            Integer durationDays,
            int quantity,
            String instructions
    ) {
        this(medicineId, dosage, frequency, route, durationDays, quantity, instructions, null);
    }
}
