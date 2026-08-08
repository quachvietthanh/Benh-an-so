package com.benhsoan.port.dto.command.medicine;

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
        int minStockThreshold
) {
}
