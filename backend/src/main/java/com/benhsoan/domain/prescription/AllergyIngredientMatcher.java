package com.benhsoan.domain.prescription;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AllergyIngredientMatcher {

    private AllergyIngredientMatcher() {
    }

    /**
     * Checks if a medicine's active ingredient matches a patient's documented allergen.
     *
     * @param activeIngredient       the active ingredient(s) of the medicine (e.g., "Amoxicillin 500mg, Acid Clavulanic 125mg")
     * @param normalizedAllergenName the normalized allergen name of the patient (e.g., "amoxicillin")
     * @return true if there is a match indicating an allergy conflict
     */
    public static boolean matches(String activeIngredient, String normalizedAllergenName) {
        if (activeIngredient == null || activeIngredient.isBlank()
                || normalizedAllergenName == null || normalizedAllergenName.isBlank()) {
            return false;
        }

        String normAllergen = normalizedAllergenName.trim().toLowerCase(Locale.ROOT);
        String normIngredient = activeIngredient.trim().toLowerCase(Locale.ROOT);

        if (normIngredient.equals(normAllergen)) {
            return true;
        }

        // Check if allergen name appears as a whole word/phrase in activeIngredient
        Pattern wordPattern = Pattern.compile("(?i)(^|[^\\p{L}\\p{N}])" + Pattern.quote(normAllergen) + "([^\\p{L}\\p{N}]|$)");
        if (wordPattern.matcher(normIngredient).find()) {
            return true;
        }

        // Split active ingredient by common composite separators (e.g. ",", "+", "/", ";")
        String[] parts = normIngredient.split("[,+/;]");
        for (String part : parts) {
            String cleanPart = part.replaceAll("[0-9]+(?:\\.[0-9]+)?\\s*(?:mg|g|ml|mcg|iu|ui|%)?", "")
                    .replaceAll("[()]", " ")
                    .trim()
                    .replaceAll("\\s+", " ");

            if (cleanPart.isEmpty()) {
                continue;
            }

            if (cleanPart.equals(normAllergen)) {
                return true;
            }

            if (cleanPart.contains(normAllergen) || normAllergen.contains(cleanPart)) {
                Pattern partPattern = Pattern.compile("(?i)(^|[^\\p{L}\\p{N}])" + Pattern.quote(cleanPart) + "([^\\p{L}\\p{N}]|$)");
                if (wordPattern.matcher(cleanPart).find() || partPattern.matcher(normAllergen).find()) {
                    return true;
                }
            }
        }

        return false;
    }
}
