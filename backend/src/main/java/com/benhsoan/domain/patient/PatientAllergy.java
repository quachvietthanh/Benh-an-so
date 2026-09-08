package com.benhsoan.domain.patient;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientAllergy {

    private UUID id;
    private UUID patientId;
    private String allergenType;
    private String allergenName;
    private String normalizedAllergenName;
    private AllergySeverity severity;
    private String reaction;
    private String notes;
    private boolean active;
    private UUID createdBy;
    private Instant createdAt;
    private UUID updatedBy;
    private Instant updatedAt;

    private PatientAllergy(
            UUID id,
            UUID patientId,
            String allergenType,
            String allergenName,
            String normalizedAllergenName,
            AllergySeverity severity,
            String reaction,
            String notes,
            boolean active,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Allergy ID cannot be null");
        this.patientId = Guard.require(patientId, "Patient ID");
        this.allergenType = allergenType != null && !allergenType.isBlank() ? allergenType.trim() : "MEDICATION";
        this.allergenName = Guard.require(allergenName, "Allergen name").trim();
        this.normalizedAllergenName = normalizeAllergenName(this.allergenName);
        this.severity = Objects.requireNonNull(severity, "Allergy severity cannot be null");
        this.reaction = reaction != null ? reaction.trim() : null;
        this.notes = notes != null ? notes.trim() : null;
        this.active = active;
        this.createdBy = Guard.require(createdBy, "Created by");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.updatedBy = updatedBy;
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }

    public static String normalizeAllergenName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static PatientAllergy create(
            UUID patientId,
            String allergenType,
            String allergenName,
            AllergySeverity severity,
            String reaction,
            String notes,
            UUID createdBy,
            Instant createdAt
    ) {
        String trimmedName = Guard.require(allergenName, "Allergen name").trim();
        Instant now = createdAt != null ? createdAt : Instant.now();
        return new PatientAllergy(
                UUID.randomUUID(),
                patientId,
                allergenType,
                trimmedName,
                normalizeAllergenName(trimmedName),
                severity,
                reaction,
                notes,
                true,
                createdBy,
                now,
                null,
                now
        );
    }

    public static PatientAllergy restore(
            UUID id,
            UUID patientId,
            String allergenType,
            String allergenName,
            String normalizedAllergenName,
            AllergySeverity severity,
            String reaction,
            String notes,
            boolean active,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        return new PatientAllergy(
                id,
                patientId,
                allergenType,
                allergenName,
                normalizedAllergenName,
                severity,
                reaction,
                notes,
                active,
                createdBy,
                createdAt,
                updatedBy,
                updatedAt
        );
    }

    public void update(
            String allergenName,
            AllergySeverity severity,
            String reaction,
            String notes,
            UUID updatedBy,
            Instant updatedAt
    ) {
        if (allergenName != null && !allergenName.isBlank()) {
            this.allergenName = allergenName.trim();
            this.normalizedAllergenName = normalizeAllergenName(this.allergenName);
        }
        if (severity != null) {
            this.severity = severity;
        }
        this.reaction = reaction != null ? reaction.trim() : null;
        this.notes = notes != null ? notes.trim() : null;
        this.updatedBy = Guard.require(updatedBy, "Updated by");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }

    public void deactivate(UUID updatedBy, Instant updatedAt) {
        this.active = false;
        this.updatedBy = Guard.require(updatedBy, "Updated by");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }
}
