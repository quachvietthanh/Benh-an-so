package com.benhsoan.application.ucservice.inventory;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.port.dto.result.LowStockMedicineResult;

@Component
public class LowStockEvaluator {

    public int calculateEligibleStockQuantity(List<MedicineBatch> batches, LocalDate today) {
        if (batches == null || batches.isEmpty()) {
            return 0;
        }

        return batches.stream()
                .filter(batch -> batch.isEligibleForDispenseOn(today))
                .mapToInt(MedicineBatch::getQuantity)
                .sum();
    }

    public boolean isLowStock(Medicine medicine, int eligibleStockQuantity) {
        return isLowStockByThreshold(eligibleStockQuantity, medicine.getMinStockThreshold());
    }

    public boolean isLowStockByThreshold(int eligibleStockQuantity, int minStockThreshold) {
        return eligibleStockQuantity < minStockThreshold;
    }

    public int calculateShortageQuantity(Medicine medicine, int eligibleStockQuantity) {
        return Math.max(0, medicine.getMinStockThreshold() - eligibleStockQuantity);
    }

    public LowStockMedicineResult toLowStockResult(Medicine medicine, int eligibleStockQuantity) {
        return new LowStockMedicineResult(
                medicine.getId(),
                medicine.getMedicineCode(),
                medicine.getMedicineName(),
                medicine.getUnit(),
                medicine.getStockQuantity(),
                eligibleStockQuantity,
                medicine.getMinStockThreshold(),
                calculateShortageQuantity(medicine, eligibleStockQuantity)
        );
    }
}
