package com.benhsoan.port.dto.result;

import java.util.List;

public record MaxDailyDoseCheckResult(
        List<MaxDailyDoseWarningResult> warnings,
        List<MaxDailyDoseMissingDataResult> missingData
) {
}
