package com.benhsoan.domain.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.shared.exception.ValidationException;

class ClinicalReferenceRangeTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");
    private static final UUID SERVICE_ID = UUID.randomUUID();

    @Test
    void acceptsBothBoundsAndExposesThem() {
        ClinicalReferenceRange range = range(Gender.MALE, 18, 64, "5", "10");
        assertEquals(new BigDecimal("5"), range.getLowerBound());
        assertEquals(new BigDecimal("10"), range.getUpperBound());
        assertEquals("5-10", range.toDisplayRange());
    }

    @Test
    void rejectsLowerGreaterThanUpper() {
        assertThrows(ValidationException.class, () -> range(Gender.MALE, 18, 64, "10", "5"));
    }

    @Test
    void acceptsEqualBounds() {
        ClinicalReferenceRange range = range(Gender.MALE, null, null, "5", "5");
        assertEquals(new BigDecimal("5"), range.getLowerBound());
        assertEquals(new BigDecimal("5"), range.getUpperBound());
    }

    @Test
    void acceptsLowerOnlyBound() {
        ClinicalReferenceRange range = range(Gender.FEMALE, null, null, "5", null);
        assertEquals(new BigDecimal("5"), range.getLowerBound());
        assertNull(range.getUpperBound());
    }

    @Test
    void acceptsUpperOnlyBound() {
        ClinicalReferenceRange range = range(Gender.FEMALE, null, null, null, "15");
        assertNull(range.getLowerBound());
        assertEquals(new BigDecimal("15"), range.getUpperBound());
    }

    @Test
    void rejectsBothNullBounds() {
        assertThrows(ValidationException.class, () -> range(Gender.MALE, null, null, null, null));
    }

    @Test
    void rejectsNegativeMinAge() {
        assertThrows(ValidationException.class, () -> range(Gender.MALE, -1, 10, "5", "10"));
    }

    @Test
    void rejectsNegativeMaxAge() {
        assertThrows(ValidationException.class, () -> range(Gender.MALE, 0, -1, "5", "10"));
    }

    @Test
    void rejectsMinAgeGreaterThanMaxAge() {
        assertThrows(ValidationException.class, () -> range(Gender.MALE, 30, 20, "5", "10"));
    }

    @Test
    void coversAgeInclusively() {
        ClinicalReferenceRange range = range(Gender.MALE, 18, 64, "5", "10");
        assertTrue(range.coversAge(18));
        assertTrue(range.coversAge(64));
        assertFalse(range.coversAge(17));
        assertFalse(range.coversAge(65));
    }

    @Test
    void unboundedAgeCoversEverything() {
        ClinicalReferenceRange range = range(Gender.MALE, null, null, "5", "10");
        assertTrue(range.coversAge(0));
        assertTrue(range.coversAge(150));
    }

    @Test
    void overlappingSameGenderRangesConflict() {
        ClinicalReferenceRange a = range(Gender.MALE, 18, 30, "5", "10");
        ClinicalReferenceRange b = range(Gender.MALE, 25, 40, "5", "10");
        assertTrue(a.overlaps(b));
    }

    @Test
    void differentGendersDoNotConflict() {
        ClinicalReferenceRange male = range(Gender.MALE, 18, 30, "5", "10");
        ClinicalReferenceRange female = range(Gender.FEMALE, 18, 30, "5", "10");
        assertFalse(male.overlaps(female));
    }

    @Test
    void adjacentRangesDoNotOverlap() {
        ClinicalReferenceRange child = range(Gender.MALE, 0, 17, "5", "10");
        ClinicalReferenceRange adult = range(Gender.MALE, 18, 64, "5", "10");
        assertFalse(child.overlaps(adult));
    }

    @Test
    void gappedRangesDoNotOverlap() {
        ClinicalReferenceRange child = range(Gender.MALE, 0, 17, "5", "10");
        ClinicalReferenceRange adult = range(Gender.MALE, 20, 64, "5", "10");
        assertFalse(child.overlaps(adult));
    }

    @Test
    void genericAndSpecificGendersMayCoexist() {
        ClinicalReferenceRange generic = range(null, 0, 120, "5", "10");
        ClinicalReferenceRange male = range(Gender.MALE, 18, 64, "5", "10");
        assertFalse(generic.overlaps(male));
        assertTrue(generic.isGenericGender());
        assertTrue(male.matchesGender(Gender.MALE));
    }

    private ClinicalReferenceRange range(Gender gender, Integer minAge, Integer maxAge, String lower, String upper) {
        return ClinicalReferenceRange.create(SERVICE_ID, gender, minAge, maxAge, dec(lower), dec(upper), NOW);
    }

    private static BigDecimal dec(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
