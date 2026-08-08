package com.benhsoan.port.dto.result;

import java.util.UUID;

public record LowStockMedicineResult(
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String unit,
        int stockQuantity,
        int eligibleStockQuantity,
        int minStockThreshold,
        int shortageQuantity
) {
}
