package com.benhsoan.domain.prescription;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * NCL-05-CN-007: pure calculation of total daily active-ingredient dose.
 *
 * <p>Formula (authoritative): daily dose = strengthValueMg × singleDoseQuantity ×
 * frequencyPerDay, aggregated per active ingredient and compared against the
 * active-ingredient level {@code maxDailyDoseMg} (mg/day).
 *
 * <p>Configuration consistency rule: for a given active ingredient, the maximum
 * daily dose is usable only when every dose-carrying row agrees on a single
 * non-null value. Null/inconsistent configuration is reported as missing data and
 * never blocks the prescription (TC-04).
 *
 * <p>This class is framework-, DTO-, persistence- and HTTP-independent and returns
 * only domain-owned result types.
 */
public final class MaxDailyDoseCalculator {

    private MaxDailyDoseCalculator() {
    }

    public record Item(
            String activeIngredient,
            BigDecimal strengthValueMg,
            BigDecimal singleDoseQuantity,
            int frequencyPerDay,
            BigDecimal maxDailyDoseMg
    ) {
    }

    public static MaxDailyDoseEvaluationResult evaluate(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return new MaxDailyDoseEvaluationResult(List.of(), List.of());
        }

        // Group by normalized active ingredient name while preserving insertion order.
        Map<String, List<Item>> byIngredient = new LinkedHashMap<>();
        for (Item item : items) {
            String key = normalize(item.activeIngredient());
            byIngredient.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }

        List<MaxDailyDoseWarning> warnings = new ArrayList<>();
        List<MaxDailyDoseMissingData> missingData = new ArrayList<>();

        for (Map.Entry<String, List<Item>> entry : byIngredient.entrySet()) {
            String ingredient = entry.getValue().getFirst().activeIngredient();
            List<Item> group = entry.getValue();

            // Resolve the active-ingredient level maxDailyDoseMg deterministically.
            BigDecimal maxDose = resolveMaxDailyDose(group);
            if (maxDose == null) {
                missingData.add(new MaxDailyDoseMissingData(
                        ingredient,
                        "Thiếu hoặc không nhất quán cấu hình liều tối đa theo ngày cho hoạt chất."
                ));
                continue;
            }

            // Every item must have complete, positive dose inputs (missing is NOT zero).
            BigDecimal total = BigDecimal.ZERO;
            boolean incomplete = false;
            for (Item item : group) {
                if (item.strengthValueMg() == null
                        || item.singleDoseQuantity() == null
                        || item.frequencyPerDay() <= 0) {
                    incomplete = true;
                    break;
                }
                BigDecimal perDose = item.strengthValueMg().multiply(item.singleDoseQuantity());
                BigDecimal daily = perDose.multiply(BigDecimal.valueOf(item.frequencyPerDay()));
                total = total.add(daily);
            }

            if (incomplete) {
                missingData.add(new MaxDailyDoseMissingData(
                        ingredient,
                        "Thiếu dữ liệu liều (hàm lượng mg, số lượng mỗi lần hoặc số lần/ngày) cho hoạt chất."
                ));
                continue;
            }

            if (total.compareTo(maxDose) > 0) {
                warnings.add(new MaxDailyDoseWarning(ingredient, total, maxDose));
            }
        }

        return new MaxDailyDoseEvaluationResult(List.copyOf(warnings), List.copyOf(missingData));
    }

    /**
     * Returns the single non-null maxDailyDoseMg for an ingredient group, or null when
     * the group has no value (unconfigured), has mixed null/non-null values (incomplete),
     * or has conflicting non-null values (configuration conflict). Null is never treated
     * as zero or unlimited; it is reported as missing data.
     *
     * <p>Distinct values are compared numerically ({@code BigDecimal.compareTo}), so
     * {@code 2000.000} and {@code 2000} are the same configuration rather than a conflict.
     */
    private static BigDecimal resolveMaxDailyDose(List<Item> group) {
        TreeSet<BigDecimal> distinct = new TreeSet<>(BigDecimal::compareTo);
        boolean sawNull = false;
        for (Item item : group) {
            if (item.maxDailyDoseMg() == null) {
                sawNull = true;
            } else {
                distinct.add(item.maxDailyDoseMg());
            }
        }

        if (distinct.size() == 1 && !sawNull) {
            return distinct.first();
        }
        // 0 values, mixed null/non-null, or >1 distinct non-null values → missing data.
        return null;
    }

    private static String normalize(String activeIngredient) {
        return activeIngredient == null
                ? ""
                : activeIngredient.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
