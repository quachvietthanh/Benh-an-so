package com.benhsoan.domain.prescription;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("AllergyIngredientMatcher Unit Tests")
class AllergyIngredientMatcherTest {

    @Test
    void matchesExactSingleIngredientCaseInsensitive() {
        assertTrue(AllergyIngredientMatcher.matches("Amoxicillin", "amoxicillin"));
        assertTrue(AllergyIngredientMatcher.matches("AMOXICILLIN", "Amoxicillin"));
        assertTrue(AllergyIngredientMatcher.matches("amoxicillin", "AMOXICILLIN"));
    }

    @Test
    void matchesSingleIngredientWithStrengthAndUnit() {
        assertTrue(AllergyIngredientMatcher.matches("Amoxicillin 500mg", "amoxicillin"));
        assertTrue(AllergyIngredientMatcher.matches("Paracetamol 500 mg", "paracetamol"));
        assertTrue(AllergyIngredientMatcher.matches("Ibuprofen 400 mg film-coated", "ibuprofen"));
        assertTrue(AllergyIngredientMatcher.matches("Insulin 100 IU/ml", "insulin"));
    }

    @Test
    void matchesCompositeIngredientsWithVariousSeparators() {
        // Separated by comma
        assertTrue(AllergyIngredientMatcher.matches("Amoxicillin 500mg, Acid Clavulanic 125mg", "amoxicillin"));
        assertTrue(AllergyIngredientMatcher.matches("Amoxicillin 500mg, Acid Clavulanic 125mg", "acid clavulanic"));

        // Separated by plus
        assertTrue(AllergyIngredientMatcher.matches("Paracetamol 500mg + Ibuprofen 200mg", "ibuprofen"));
        assertTrue(AllergyIngredientMatcher.matches("Paracetamol 500mg + Codein 30mg", "codein"));

        // Separated by slash
        assertTrue(AllergyIngredientMatcher.matches("Sulfamethoxazole / Trimethoprim", "trimethoprim"));
        assertTrue(AllergyIngredientMatcher.matches("Sulfamethoxazole / Trimethoprim", "sulfamethoxazole"));

        // Separated by semicolon
        assertTrue(AllergyIngredientMatcher.matches("Vitamin B1 100mg; Vitamin B6 200mg; Vitamin B12 200mcg", "vitamin b6"));
    }

    @ParameterizedTest
    @CsvSource({
            "Paracetamol 500mg, amoxicillin",
            "Aspirin 81mg, penicillin",
            "Cefalexin 500mg, ciprofloxacin"
    })
    void doesNotMatchDifferentIngredients(String medicineIngredient, String allergen) {
        assertFalse(AllergyIngredientMatcher.matches(medicineIngredient, allergen));
    }

    @Test
    void handlesNullOrBlankInputsGracefully() {
        assertFalse(AllergyIngredientMatcher.matches(null, "amoxicillin"));
        assertFalse(AllergyIngredientMatcher.matches("Amoxicillin", null));
        assertFalse(AllergyIngredientMatcher.matches("", "amoxicillin"));
        assertFalse(AllergyIngredientMatcher.matches("Amoxicillin", "   "));
        assertFalse(AllergyIngredientMatcher.matches(null, null));
    }
}
