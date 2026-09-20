package com.benhsoan.domain.contraindication;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContraindicationRule {

    private UUID id;
    private UUID medicineId;
    private String activeIngredient;
    private ContraindicationType type;
    private Integer minAgeYears;
    private Integer maxAgeYears;
    private UUID diagnosisCatalogId;
    private ContraindicationSeverity severity;
    private String message;
    private String recommendation;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    private ContraindicationRule(
            UUID id,
            UUID medicineId,
            String activeIngredient,
            ContraindicationType type,
            Integer minAgeYears,
            Integer maxAgeYears,
            UUID diagnosisCatalogId,
            ContraindicationSeverity severity,
            String message,
            String recommendation,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = requireNonNull(id, "Contraindication rule id is required.");
        this.medicineId = medicineId;
        this.activeIngredient = normalizeOptionalText(activeIngredient);
        this.type = requireNonNull(type, "Contraindication type is required.");
        this.minAgeYears = minAgeYears;
        this.maxAgeYears = maxAgeYears;
        this.diagnosisCatalogId = diagnosisCatalogId;
        this.severity = requireNonNull(severity, "Contraindication severity is required.");
        this.message = requireText(message, "Contraindication message is required.");
        this.recommendation = normalizeOptionalText(recommendation);
        this.active = active;
        this.createdAt = requireNonNull(createdAt, "Contraindication rule creation time is required.");
        this.updatedAt = updatedAt;
        validate();
    }

    public static ContraindicationRule restore(
            UUID id,
            UUID medicineId,
            String activeIngredient,
            ContraindicationType type,
            Integer minAgeYears,
            Integer maxAgeYears,
            UUID diagnosisCatalogId,
            ContraindicationSeverity severity,
            String message,
            String recommendation,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ContraindicationRule(
                id, medicineId, activeIngredient, type, minAgeYears, maxAgeYears,
                diagnosisCatalogId, severity, message, recommendation, active, createdAt, updatedAt
        );
    }

    private void validate() {
        if (medicineId == null && activeIngredient == null) {
            throw new ValidationException(
                    "A contraindication rule must target a medicine or an active ingredient.");
        }

        switch (type) {
            case AGE -> {
                if (minAgeYears == null && maxAgeYears == null) {
                    throw new ValidationException(
                            "An age contraindication rule requires at least one age bound.");
                }
                if (minAgeYears != null && maxAgeYears != null && minAgeYears > maxAgeYears) {
                    throw new ValidationException(
                            "Age contraindication min age must not exceed max age.");
                }
            }
            case PREGNANCY -> {
                if (diagnosisCatalogId != null) {
                    throw new ValidationException(
                            "A pregnancy contraindication rule must not reference a diagnosis.");
                }
            }
            case DISEASE -> {
                if (diagnosisCatalogId == null) {
                    throw new ValidationException(
                            "A disease contraindication rule requires a diagnosis catalog id.");
                }
            }
        }
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String message) {
        if (Objects.isNull(value)) {
            throw new ValidationException(message);
        }
        return value;
    }
}
