package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.List;

public record MaxDailyDoseCheckResponse(
        List<MaxDailyDoseWarningResponse> warnings,
        List<MaxDailyDoseMissingDataResponse> missingData
) {
}
