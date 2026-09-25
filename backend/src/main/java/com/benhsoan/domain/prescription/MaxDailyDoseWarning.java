package com.benhsoan.domain.prescription;

import java.math.BigDecimal;

/**
 * NCL-05-CN-007: domain-owned result of a max-daily-dose exceedance for a single
 * active ingredient (aggregated across all dose-carrying items).
 */
public record MaxDailyDoseWarning(
        String activeIngredient,
        BigDecimal totalDailyDoseMg,
        BigDecimal maxDailyDoseMg
) {
}
