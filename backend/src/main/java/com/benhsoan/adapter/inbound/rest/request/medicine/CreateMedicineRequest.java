package com.benhsoan.adapter.inbound.rest.request.medicine;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateMedicineRequest(
        @NotBlank(message = "Medicine code is required.")
        @Size(max = 50, message = "Medicine code must not exceed 50 characters.")
        String medicineCode,

        @NotBlank(message = "Medicine name is required.")
        @Size(max = 255, message = "Medicine name must not exceed 255 characters.")
        String medicineName,

        @NotBlank(message = "Active ingredient is required.")
        @Size(max = 255, message = "Active ingredient must not exceed 255 characters.")
        String activeIngredient,

        @NotBlank(message = "Medicine strength is required.")
        @Size(max = 100, message = "Medicine strength must not exceed 100 characters.")
        String strength,

        @NotNull(message = "Dosage form is required.")
        DosageForm dosageForm,

        @NotBlank(message = "Medicine unit is required.")
        @Size(max = 50, message = "Medicine unit must not exceed 50 characters.")
        String unit,

        @NotNull(message = "Default administration route is required.")
        AdministrationRoute defaultRoute,

        @PositiveOrZero(message = "Minimum stock threshold must be greater than or equal to 0.")
        int minStockThreshold
) {
}
