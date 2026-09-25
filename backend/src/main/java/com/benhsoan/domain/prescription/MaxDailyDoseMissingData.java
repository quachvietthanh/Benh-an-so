package com.benhsoan.domain.prescription;

/**
 * NCL-05-CN-007: domain-owned missing-data report for an active ingredient whose
 * max-daily-dose configuration or dose inputs are missing/inconsistent. Missing
 * data never blocks the prescription (TC-04).
 */
public record MaxDailyDoseMissingData(
        String activeIngredient,
        String reason
) {
}
