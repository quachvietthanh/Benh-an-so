package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CheckPatientDrugAllergyRequest(

        @NotNull
        UUID medicalRecordId,

        @NotEmpty
        List<@NotNull UUID> medicineIds

) {
}
