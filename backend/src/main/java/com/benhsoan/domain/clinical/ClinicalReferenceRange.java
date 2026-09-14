package com.benhsoan.domain.clinical;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single gender- and age-specific reference threshold for a clinical service.
 *
 * <p>{@code gender == null} represents a generic "applies to all genders" range.
 * {@code minAge}/{@code maxAge} are inclusive and measured in whole years;
 * {@code null} means unbounded. {@code lowerBound}/{@code upperBound} are the
 * numeric threshold bounds; {@code null} means unbounded on that side.</p>
 */
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalReferenceRange {

    private UUID id;
    private UUID clinicalServiceId;
    private Gender gender;
    private Integer minAge;
    private Integer maxAge;
    private BigDecimal lowerBound;
    private BigDecimal upperBound;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    private ClinicalReferenceRange(
            UUID id,
            UUID clinicalServiceId,
            Gender gender,
            Integer minAge,
            Integer maxAge,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.clinicalServiceId = Objects.requireNonNull(clinicalServiceId);
        this.gender = gender;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = updatedAt;
        validate();
    }

    public static ClinicalReferenceRange create(
            UUID clinicalServiceId,
            Gender gender,
            Integer minAge,
            Integer maxAge,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            Instant createdAt
    ) {
        return new ClinicalReferenceRange(
                UUID.randomUUID(), clinicalServiceId, gender, minAge, maxAge,
                lowerBound, upperBound, true, Objects.requireNonNull(createdAt), null
        );
    }

    public static ClinicalReferenceRange restore(
            UUID id,
            UUID clinicalServiceId,
            Gender gender,
            Integer minAge,
            Integer maxAge,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ClinicalReferenceRange(
                id, clinicalServiceId, gender, minAge, maxAge,
                lowerBound, upperBound, active, createdAt, updatedAt
        );
    }
    public void updateInformation(
            Gender gender,
            Integer minAge,
            Integer maxAge,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            Instant at
    ) {
        this.gender = gender;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.updatedAt = Objects.requireNonNull(at);
        validate();
    }

    public void activate(Instant at) {
        this.active = true;
        this.updatedAt = Objects.requireNonNull(at);
    }

    public void deactivate(Instant at) {
        this.active = false;
        this.updatedAt = Objects.requireNonNull(at);
    }

    public boolean coversAge(int age) {
        if (minAge != null && age < minAge) {
            return false;
        }
        if (maxAge != null && age > maxAge) {
            return false;
        }
        return true;
    }

    public boolean isGenericGender() {
        return gender == null;
    }

    public boolean matchesGender(Gender patientGender) {
        return gender != null && gender == patientGender;
    }

    /**
     * Two ranges conflict when they share the same gender bucket and their
     * inclusive age intervals intersect.
     */
    public boolean overlaps(ClinicalReferenceRange other) {
        if (!sameGenderBucket(other)) {
            return false;
        }
        long minA = minAge == null ? Long.MIN_VALUE : minAge;
        long maxA = maxAge == null ? Long.MAX_VALUE : maxAge;
        long minB = other.minAge == null ? Long.MIN_VALUE : other.minAge;
        long maxB = other.maxAge == null ? Long.MAX_VALUE : other.maxAge;
        return minA <= maxB && minB <= maxA;
    }

    private boolean sameGenderBucket(ClinicalReferenceRange other) {
        if (gender == null && other.gender == null) {
            return true;
        }
        return gender != null && gender.equals(other.gender);
    }

    public String toDisplayRange() {
        if (lowerBound != null && upperBound != null) {
            return plain(lowerBound) + "-" + plain(upperBound);
        }
        if (lowerBound != null) {
            return ">=" + plain(lowerBound);
        }
        if (upperBound != null) {
            return "<=" + plain(upperBound);
        }
        return null;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private void validate() {
        if (minAge != null && minAge < 0) {
            throw new ValidationException("Minimum age must not be negative.");
        }
        if (maxAge != null && maxAge < 0) {
            throw new ValidationException("Maximum age must not be negative.");
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new ValidationException("Minimum age must not exceed maximum age.");
        }
        if (lowerBound == null && upperBound == null) {
            throw new ValidationException("Reference range requires at least one bound.");
        }
        if (lowerBound != null && upperBound != null && lowerBound.compareTo(upperBound) > 0) {
            throw new ValidationException("Lower bound must not exceed upper bound.");
        }
    }
}
