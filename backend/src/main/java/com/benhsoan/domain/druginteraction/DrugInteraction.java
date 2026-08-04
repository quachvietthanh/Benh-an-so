package com.benhsoan.domain.druginteraction;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.druginteraction.exception.SelfDrugInteractionException;
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
public class DrugInteraction {

    private UUID id;

    private UUID firstMedicineId;

    private UUID secondMedicineId;

    private InteractionSeverity severity;

    private String description;

    private String recommendation;

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    private DrugInteraction(
            UUID id,
            UUID firstMedicineId,
            UUID secondMedicineId,
            InteractionSeverity severity,
            String description,
            String recommendation,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = requireNonNull(id, "Drug interaction id is required.");
        UUID validatedFirstMedicineId = requireNonNull(firstMedicineId, "First medicine id is required.");
        UUID validatedSecondMedicineId = requireNonNull(secondMedicineId, "Second medicine id is required.");
        validateDifferentMedicines(validatedFirstMedicineId, validatedSecondMedicineId);

        if (validatedFirstMedicineId.compareTo(validatedSecondMedicineId) <= 0) {
            this.firstMedicineId = validatedFirstMedicineId;
            this.secondMedicineId = validatedSecondMedicineId;
        } else {
            this.firstMedicineId = validatedSecondMedicineId;
            this.secondMedicineId = validatedFirstMedicineId;
        }

        this.severity = requireNonNull(severity, "Interaction severity is required.");
        this.description = requireText(description, "Interaction description is required.");
        this.recommendation = requireText(recommendation, "Interaction recommendation is required.");
        this.active = active;
        this.createdAt = requireNonNull(createdAt, "Drug interaction creation time is required.");
        this.updatedAt = updatedAt;
    }

    public static DrugInteraction create(
            UUID id,
            UUID firstMedicineId,
            UUID secondMedicineId,
            InteractionSeverity severity,
            String description,
            String recommendation,
            Instant createdAt
    ) {
        return new DrugInteraction(
                id,
                firstMedicineId,
                secondMedicineId,
                severity,
                description,
                recommendation,
                true,
                createdAt,
                null
        );
    }

    public static DrugInteraction restore(
            UUID id,
            UUID firstMedicineId,
            UUID secondMedicineId,
            InteractionSeverity severity,
            String description,
            String recommendation,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new DrugInteraction(
                id,
                firstMedicineId,
                secondMedicineId,
                severity,
                description,
                recommendation,
                active,
                createdAt,
                updatedAt
        );
    }

    public void updateDetails(
            InteractionSeverity severity,
            String description,
            String recommendation,
            Instant updatedAt
    ) {
        InteractionSeverity validatedSeverity = requireNonNull(severity, "Interaction severity is required.");
        String validatedDescription = requireText(description, "Interaction description is required.");
        String validatedRecommendation = requireText(
                recommendation,
                "Interaction recommendation is required."
        );
        Instant validatedUpdatedAt = requireNonNull(updatedAt, "Drug interaction update time is required.");

        this.severity = validatedSeverity;
        this.description = validatedDescription;
        this.recommendation = validatedRecommendation;
        this.updatedAt = validatedUpdatedAt;
    }

    public void activate(Instant updatedAt) {
        Instant validatedUpdatedAt = requireNonNull(updatedAt, "Drug interaction update time is required.");
        this.active = true;
        this.updatedAt = validatedUpdatedAt;
    }

    public void deactivate(Instant updatedAt) {
        Instant validatedUpdatedAt = requireNonNull(updatedAt, "Drug interaction update time is required.");
        this.active = false;
        this.updatedAt = validatedUpdatedAt;
    }

    public boolean involves(UUID medicineId) {
        return firstMedicineId.equals(medicineId) || secondMedicineId.equals(medicineId);
    }

    public boolean matches(UUID medicineId, UUID otherMedicineId) {
        return firstMedicineId.equals(medicineId) && secondMedicineId.equals(otherMedicineId)
                || firstMedicineId.equals(otherMedicineId) && secondMedicineId.equals(medicineId);
    }

    private static void validateDifferentMedicines(UUID firstMedicineId, UUID secondMedicineId) {
        if (firstMedicineId.equals(secondMedicineId)) {
            throw new SelfDrugInteractionException();
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
