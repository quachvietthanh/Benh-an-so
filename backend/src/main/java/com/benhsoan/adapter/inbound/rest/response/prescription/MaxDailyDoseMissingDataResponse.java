package com.benhsoan.adapter.inbound.rest.response.prescription;

public record MaxDailyDoseMissingDataResponse(
        String activeIngredient,
        String reason
) {
}
