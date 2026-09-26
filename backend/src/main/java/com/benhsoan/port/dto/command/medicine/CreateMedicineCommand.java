package com.benhsoan.port.dto.command.medicine;

import java.math.BigDecimal;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;

public record CreateMedicineCommand(
        String medicineCode,
        String medicineName,
        String activeIngredient,
        String strength,
        DosageForm dosageForm,
        String unit,
        AdministrationRoute defaultRoute,
        int minStockThreshold,
        boolean controlled,
        BigDecimal strengthValueMg,
        BigDecimal maxDailyDoseMg
) {
    public CreateMedicineCommand(
            String medicineCode,
            String medicineName,
            String activeIngredient,
            String strength,
            DosageForm dosageForm,
            String unit,
            AdministrationRoute defaultRoute,
            int minStockThreshold
    ) {
        this(medicineCode, medicineName, activeIngredient, strength, dosageForm, unit, defaultRoute, minStockThreshold, false, null, null);
    }

    public CreateMedicineCommand(
            String medicineCode,
            String medicineName,
            String activeIngredient,
            String strength,
            DosageForm dosageForm,
            String unit,
            AdministrationRoute defaultRoute,
            int minStockThreshold,
            boolean controlled
    ) {
        this(medicineCode, medicineName, activeIngredient, strength, dosageForm, unit, defaultRoute, minStockThreshold, controlled, null, null);
    }
}
