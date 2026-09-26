package com.benhsoan.port.outbound.repository.reporting;

import java.util.UUID;

public record TopMedicineSummary(
        UUID medicineId,
        String medicineCode,
        String medicineName,
        long totalDispensedQuantity
) {
}
