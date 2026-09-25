package com.benhsoan.domain.prescription;

import java.util.List;

/**
 * NCL-05-CN-007: domain-owned evaluation result of the max-daily-dose check.
 */
public record MaxDailyDoseEvaluationResult(
        List<MaxDailyDoseWarning> warnings,
        List<MaxDailyDoseMissingData> missingData
) {
    public MaxDailyDoseEvaluationResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        missingData = missingData == null ? List.of() : List.copyOf(missingData);
    }
}
