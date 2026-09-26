package com.benhsoan.domain.druginteraction;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
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
public class DrugInteractionRule {

    private UUID id;

    private String activeIngredientA;

    private String activeIngredientB;

    private InteractionSeverity severity;

    private String description;

    private String clinicalRecommendation;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    private DrugInteractionRule(
            UUID id,
            String activeIngredientA,
            String activeIngredientB,
            InteractionSeverity severity,
            String description,
            String clinicalRecommendation,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = requireNonNull(id, "Drug interaction rule id is required.");
        this.activeIngredientA = requireText(activeIngredientA, "Active ingredient A is required.");
        this.activeIngredientB = requireText(activeIngredientB, "Active ingredient B is required.");
        validateDifferentIngredients(activeIngredientA, activeIngredientB);
        this.severity = requireNonNull(severity, "Interaction severity is required.");
        this.description = requireText(description, "Interaction description is required.");
        this.clinicalRecommendation = requireText(
                clinicalRecommendation,
                "Clinical recommendation is required."
        );
        this.active = active;
        this.createdAt = requireNonNull(createdAt, "Drug interaction rule creation time is required.");
        this.updatedAt = updatedAt;
    }

    public static DrugInteractionRule create(
            UUID id,
            String activeIngredientA,
            String activeIngredientB,
            InteractionSeverity severity,
            String description,
            String clinicalRecommendation,
            Instant createdAt
    ) {
        return new DrugInteractionRule(
                id,
                activeIngredientA,
                activeIngredientB,
                severity,
                description,
                clinicalRecommendation,
                true,
                createdAt,
                null
        );
    }

    public static DrugInteractionRule restore(
            UUID id,
            String activeIngredientA,
            String activeIngredientB,
            InteractionSeverity severity,
            String description,
            String clinicalRecommendation,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new DrugInteractionRule(
                id,
                activeIngredientA,
                activeIngredientB,
                severity,
                description,
                clinicalRecommendation,
                active,
                createdAt,
                updatedAt
        );
    }

    private static void validateDifferentIngredients(
            String activeIngredientA,
            String activeIngredientB
    ) {
        if (activeIngredientA.equalsIgnoreCase(activeIngredientB)) {
            throw new ValidationException(
                    "A drug interaction rule must reference two different active ingredients."
            );
        }
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
