package com.benhsoan.port.dto.result;

import java.math.BigDecimal;

public record MaxDailyDoseWarningResult(
        String activeIngredient,
        BigDecimal totalDailyDoseMg,
        BigDecimal maxDailyDoseMg
) {
}
