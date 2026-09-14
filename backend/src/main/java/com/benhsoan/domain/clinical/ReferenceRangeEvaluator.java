package com.benhsoan.domain.clinical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.patient.enums.Gender;

/**
 * Pure domain logic for resolving a reference range against a patient's
 * demographics and for evaluating a numeric value against resolved bounds.
 *
 * <p>No I/O is performed here; the application layer supplies the active
 * ranges and the patient's gender/age.</p>
 */
public class ReferenceRangeEvaluator {

    /**
     * Calendar age in whole years at the given reference date.
     */
    public int ageInYears(LocalDate dateOfBirth, LocalDate referenceDate) {
        return Period.between(dateOfBirth, referenceDate).getYears();
    }

    /**
     * Resolves the applicable range for a patient. A gender-specific range
     * takes precedence over a generic ({@code gender == null}) range. When no
     * range covers the patient's age, {@link Optional#empty()} is returned.
     */
    public Optional<ClinicalReferenceRange> resolve(
            Gender patientGender,
            int age,
            List<ClinicalReferenceRange> ranges
    ) {
        List<ClinicalReferenceRange> candidates = ranges.stream()
                .filter(ClinicalReferenceRange::isActive)
                .filter(range -> range.coversAge(age))
                .sorted(Comparator.comparing(ClinicalReferenceRange::getId))
                .toList();

        Optional<ClinicalReferenceRange> specific = candidates.stream()
                .filter(range -> range.matchesGender(patientGender))
                .findFirst();
        if (specific.isPresent()) {
            return specific;
        }
        return candidates.stream()
                .filter(ClinicalReferenceRange::isGenericGender)
                .findFirst();
    }

    /**
     * Evaluates a numeric value against resolved bounds.
     *
     * <p>{@code null} bound means unbounded on that side. A value equal to a
     * bound is considered normal (inclusive).</p>
     */
    public ClinicalResultAbnormalFlag evaluate(
            BigDecimal value,
            BigDecimal lowerBound,
            BigDecimal upperBound
    ) {
        if (lowerBound != null && value.compareTo(lowerBound) < 0) {
            return ClinicalResultAbnormalFlag.LOW;
        }
        if (upperBound != null && value.compareTo(upperBound) > 0) {
            return ClinicalResultAbnormalFlag.HIGH;
        }
        return ClinicalResultAbnormalFlag.NORMAL;
    }
}
