package com.benhsoan.application.ucservice.inventory;

import org.springframework.stereotype.Component;

@Component
public class MedicationProcurementSuggestionCalculator {

    public int calculateSuggestedQuantity(
            int eligibleStock,
            int minStockThreshold,
            long previousPeriodConsumption
    ) {
        long safeConsumption = Math.min(Math.max(0L, previousPeriodConsumption), (long) Integer.MAX_VALUE);
        long targetStock = safeConsumption + (long) Math.max(0, minStockThreshold);
        long deficit = targetStock - (long) Math.max(0, eligibleStock);
        long safeDeficit = Math.min(Math.max(0L, deficit), (long) Integer.MAX_VALUE);
        return (int) safeDeficit;
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
