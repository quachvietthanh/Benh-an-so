package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.math.BigDecimal;

public record MaxDailyDoseWarningResponse(
        String activeIngredient,
        BigDecimal totalDailyDoseMg,
        BigDecimal maxDailyDoseMg
) {
}
