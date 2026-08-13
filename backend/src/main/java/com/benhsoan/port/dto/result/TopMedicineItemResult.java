package com.benhsoan.port.dto.result;

import java.util.UUID;

public record TopMedicineItemResult(
        UUID medicineId,
        String medicineCode,
        String medicineName,
        long totalDispensedQuantity
) {
}
