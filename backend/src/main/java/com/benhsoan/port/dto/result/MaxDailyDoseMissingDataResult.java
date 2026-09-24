package com.benhsoan.port.dto.result;

public record MaxDailyDoseMissingDataResult(
        String activeIngredient,
        String reason
) {
}
