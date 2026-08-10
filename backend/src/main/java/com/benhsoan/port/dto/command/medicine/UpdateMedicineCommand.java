package com.benhsoan.port.dto.command.medicine;

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
        int minStockThreshold
) {
}
