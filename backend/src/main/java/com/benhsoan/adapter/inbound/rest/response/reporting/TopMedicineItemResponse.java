package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.util.UUID;

public record TopMedicineItemResponse(
        Integer rank,
        UUID medicineId,
        String medicineCode,
        String medicineName,
        long totalDispensedQuantity
) {
}
