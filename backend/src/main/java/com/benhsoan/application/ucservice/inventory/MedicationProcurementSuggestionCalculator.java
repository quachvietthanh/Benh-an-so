package com.benhsoan.application.ucservice.inventory;

import org.springframework.stereotype.Component;

@Component
public class MedicationProcurementSuggestionCalculator {

    public int calculateSuggestedQuantity(
            int eligibleStock,
            int minStockThreshold,
            long previousPeriodConsumption
    ) {
        long targetStock = previousPeriodConsumption + (long) Math.max(0, minStockThreshold);
        long deficit = targetStock - (long) Math.max(0, eligibleStock);
        return (int) Math.max(0, deficit);
    }

    public boolean shouldIncludeInSuggestion(
            int eligibleStock,
            int minStockThreshold,
            int suggestedQuantity,
            boolean onlyBelowThreshold
    ) {
        if (!onlyBelowThreshold) {
            return true;
        }
        return eligibleStock < minStockThreshold || suggestedQuantity > 0;
    }
}
