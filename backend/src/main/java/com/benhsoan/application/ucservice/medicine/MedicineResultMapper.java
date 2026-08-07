package com.benhsoan.application.ucservice.medicine;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.port.dto.result.MedicineResult;

@Component
class MedicineResultMapper {

    MedicineResult toResult(Medicine medicine) {
        return new MedicineResult(
                medicine.getId(),
                medicine.getMedicineCode(),
                medicine.getMedicineName(),
                medicine.getActiveIngredient(),
                medicine.getStrength(),
                medicine.getDosageForm(),
                medicine.getUnit(),
                medicine.getDefaultRoute(),
                medicine.isActive(),
                medicine.getCreatedAt(),
                medicine.getUpdatedAt(),
                medicine.getStockQuantity()
        );
    }
}
