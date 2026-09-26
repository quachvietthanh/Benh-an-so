package com.benhsoan.domain.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.patient.enums.Gender;

class ReferenceRangeEvaluatorTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");
    private static final UUID SERVICE_ID = UUID.randomUUID();

    private final ReferenceRangeEvaluator evaluator = new ReferenceRangeEvaluator();

    @Test
    void evaluatesLowHighAndNormal() {
        assertEquals(ClinicalResultAbnormalFlag.LOW, evaluator.evaluate(new BigDecimal("4"), dec("5"), dec("10")));
        assertEquals(ClinicalResultAbnormalFlag.HIGH, evaluator.evaluate(new BigDecimal("11"), dec("5"), dec("10")));
        assertEquals(ClinicalResultAbnormalFlag.NORMAL, evaluator.evaluate(new BigDecimal("5"), dec("5"), dec("10")));
        assertEquals(ClinicalResultAbnormalFlag.NORMAL, evaluator.evaluate(new BigDecimal("10"), dec("5"), dec("10")));
    }

    @Test
    void evaluatesOneSidedRanges() {
        assertEquals(ClinicalResultAbnormalFlag.LOW, evaluator.evaluate(new BigDecimal("4"), dec("5"), null));
        assertEquals(ClinicalResultAbnormalFlag.NORMAL, evaluator.evaluate(new BigDecimal("5"), dec("5"), null));
        assertEquals(ClinicalResultAbnormalFlag.HIGH, evaluator.evaluate(new BigDecimal("16"), null, dec("15")));
        assertEquals(ClinicalResultAbnormalFlag.NORMAL, evaluator.evaluate(new BigDecimal("15"), null, dec("15")));
    }

    @Test
    void resolvesSpecificGenderOverGeneric() {
        ClinicalReferenceRange generic = range(null, 0, 120, "1", "9");
        ClinicalReferenceRange male = range(Gender.MALE, 18, 64, "5", "10");

        ClinicalReferenceRange resolved = evaluator.resolve(Gender.MALE, 30, List.of(generic, male)).orElseThrow();
        assertEquals(male.getId(), resolved.getId());
    }

    @Test
    void fallsBackToGenericWhenNoSpecificRange() {
        ClinicalReferenceRange generic = range(null, 0, 120, "5", "10");
        ClinicalReferenceRange resolved = evaluator.resolve(Gender.FEMALE, 30, List.of(generic)).orElseThrow();
        assertEquals(generic.getId(), resolved.getId());
    }

    @Test
    void treatsOtherAsItsOwnGender() {
        ClinicalReferenceRange other = range(Gender.OTHER, 0, 120, "5", "10");
        assertTrue(evaluator.resolve(Gender.OTHER, 30, List.of(other)).isPresent());
        assertTrue(evaluator.resolve(Gender.MALE, 30, List.of(other)).isEmpty());
    }

    @Test
    void returnsEmptyWhenNoRangeCoversAge() {
        ClinicalReferenceRange range = range(Gender.MALE, 18, 64, "5", "10");
        assertTrue(evaluator.resolve(Gender.MALE, 70, List.of(range)).isEmpty());
    }

    @Test
    void calculatesCalendarAgeInYears() {
        assertEquals(26, evaluator.ageInYears(LocalDate.of(2000, 1, 1), LocalDate.of(2026, 6, 1)));
        assertEquals(25, evaluator.ageInYears(LocalDate.of(2000, 12, 31), LocalDate.of(2026, 6, 1)));
    }

    private ClinicalReferenceRange range(Gender gender, Integer minAge, Integer maxAge, String lower, String upper) {
        return ClinicalReferenceRange.create(SERVICE_ID, gender, minAge, maxAge, dec(lower), dec(upper), NOW);
    }

    private static BigDecimal dec(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
