package com.benhsoan.port.dto.command.medicine;

import java.math.BigDecimal;
import java.util.UUID;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;

public record UpdateMedicineCommand(
        UUID medicineId,
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
    public UpdateMedicineCommand(
            UUID medicineId,
            String medicineName,
            String activeIngredient,
            String strength,
            DosageForm dosageForm,
            String unit,
            AdministrationRoute defaultRoute,
            int minStockThreshold
    ) {
        this(medicineId, medicineName, activeIngredient, strength, dosageForm, unit, defaultRoute, minStockThreshold, false, null, null);
    }

    public UpdateMedicineCommand(
            UUID medicineId,
            String medicineName,
            String activeIngredient,
            String strength,
            DosageForm dosageForm,
            String unit,
            AdministrationRoute defaultRoute,
            int minStockThreshold,
            boolean controlled
    ) {
        this(medicineId, medicineName, activeIngredient, strength, dosageForm, unit, defaultRoute, minStockThreshold, controlled, null, null);
    }
}
