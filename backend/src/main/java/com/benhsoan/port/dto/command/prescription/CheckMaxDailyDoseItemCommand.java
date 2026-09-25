package com.benhsoan.port.dto.command.prescription;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckMaxDailyDoseItemCommand(
        UUID medicineId,
        BigDecimal singleDoseQuantity,
        Integer frequency
) {
}
