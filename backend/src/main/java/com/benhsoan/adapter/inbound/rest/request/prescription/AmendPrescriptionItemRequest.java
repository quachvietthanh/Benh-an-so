package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AmendPrescriptionItemRequest(

        @NotNull
        UUID medicineId,

        @NotBlank
        @Size(max = 100)
        String dosage,

        @NotBlank
        @Size(max = 100)
        String frequency,

        AdministrationRoute route,

        @Positive
        Integer durationDays,

        @Positive
        int quantity,

        String instructions

) {
}
