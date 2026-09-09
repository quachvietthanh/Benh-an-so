package com.benhsoan.domain.prescription;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.shared.Guard.Guard;
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
public class PrescriptionAllergyWarningLog {

    private UUID id;
    private UUID prescriptionId;
    private UUID patientId;
    private UUID allergyId;
    private UUID medicineId;
    private String activeIngredient;
    private String allergenName;
    private AllergySeverity severity;
    private String reaction;
    private String overrideReason;
    private UUID handledBy;
    private Instant handledAt;
    private Instant createdAt;

    private PrescriptionAllergyWarningLog(
            UUID id,
            UUID prescriptionId,
            UUID patientId,
            UUID allergyId,
            UUID medicineId,
            String activeIngredient,
            String allergenName,
            AllergySeverity severity,
            String reaction,
            String overrideReason,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "Prescription allergy warning log ID cannot be null");
        this.prescriptionId = Guard.require(prescriptionId, "Prescription ID");
        this.patientId = Guard.require(patientId, "Patient ID");
        this.allergyId = Guard.require(allergyId, "Allergy ID");
        this.medicineId = Guard.require(medicineId, "Medicine ID");
        this.activeIngredient = Guard.require(activeIngredient, "Active ingredient").trim();
        this.allergenName = Guard.require(allergenName, "Allergen name").trim();
        this.severity = Objects.requireNonNull(severity, "Allergy severity cannot be null");
        this.reaction = reaction != null ? reaction.trim() : null;
        this.overrideReason = validateOverrideReason(overrideReason);
        this.handledBy = Guard.require(handledBy, "Handled by");
        this.handledAt = Objects.requireNonNull(handledAt, "Handled at cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
    }

    public static PrescriptionAllergyWarningLog create(
            UUID id,
            UUID prescriptionId,
            UUID patientId,
            UUID allergyId,
            UUID medicineId,
            String activeIngredient,
            String allergenName,
            AllergySeverity severity,
            String reaction,
            String overrideReason,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        return new PrescriptionAllergyWarningLog(
                id != null ? id : UUID.randomUUID(),
                prescriptionId,
                patientId,
                allergyId,
                medicineId,
                activeIngredient,
                allergenName,
                severity,
                reaction,
                overrideReason,
                handledBy,
                handledAt != null ? handledAt : Instant.now(),
                createdAt != null ? createdAt : Instant.now()
        );
    }

    public static PrescriptionAllergyWarningLog restore(
            UUID id,
            UUID prescriptionId,
            UUID patientId,
            UUID allergyId,
            UUID medicineId,
            String activeIngredient,
            String allergenName,
            AllergySeverity severity,
            String reaction,
            String overrideReason,
            UUID handledBy,
            Instant handledAt,
            Instant createdAt
    ) {
        return new PrescriptionAllergyWarningLog(
                id,
                prescriptionId,
                patientId,
                allergyId,
                medicineId,
                activeIngredient,
                allergenName,
                severity,
                reaction,
                overrideReason,
                handledBy,
                handledAt,
                createdAt
        );
    }

    private static String validateOverrideReason(String overrideReason) {
        if (overrideReason == null || overrideReason.isBlank()) {
            throw new ValidationException("Override reason is required when a medication allergy warning is overridden.");
        }
        return overrideReason.trim();
    }
}
