package com.benhsoan.domain.patient;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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
public class PatientFamilyHistory {

    private UUID id;
    private UUID patientId;
    private String relationship;
    private UUID diagnosisCatalogId;
    private String notes;
    private boolean active;
    private UUID createdBy;
    private Instant createdAt;
    private UUID updatedBy;
    private Instant updatedAt;

    private PatientFamilyHistory(
            UUID id,
            UUID patientId,
            String relationship,
            UUID diagnosisCatalogId,
            String notes,
            boolean active,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Family history ID cannot be null");
        this.patientId = Guard.require(patientId, "Patient ID");
        this.relationship = Guard.require(relationship, "Relationship").trim();
        this.diagnosisCatalogId = Guard.require(diagnosisCatalogId, "Diagnosis catalog ID");
        this.notes = notes != null && !notes.isBlank() ? notes.trim() : null;
        this.active = active;
        this.createdBy = Guard.require(createdBy, "Created by");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.updatedBy = updatedBy;
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }

    public static PatientFamilyHistory create(
            UUID patientId,
            String relationship,
            UUID diagnosisCatalogId,
            String notes,
            UUID createdBy,
            Instant createdAt
    ) {
        Instant now = createdAt != null ? createdAt : Instant.now();
        return new PatientFamilyHistory(
                UUID.randomUUID(),
                patientId,
                relationship,
                diagnosisCatalogId,
                notes,
                true,
                createdBy,
                now,
                null,
                now
        );
    }

    public static PatientFamilyHistory restore(
            UUID id,
            UUID patientId,
            String relationship,
            UUID diagnosisCatalogId,
            String notes,
            boolean active,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        return new PatientFamilyHistory(
                id,
                patientId,
                relationship,
                diagnosisCatalogId,
                notes,
                active,
                createdBy,
                createdAt,
                updatedBy,
                updatedAt
        );
    }

    public void deactivate(UUID updatedBy, Instant updatedAt) {
        this.active = false;
        this.updatedBy = Guard.require(updatedBy, "Updated by");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }
}
