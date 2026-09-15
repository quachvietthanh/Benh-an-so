package com.benhsoan.domain.patient;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

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
public class PatientChronicDisease {

    private static final int MIN_YEAR = 1900;
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private UUID id;
    private UUID patientId;
    private UUID diagnosisCatalogId;
    private Integer yearDetected;
    private String notes;
    private boolean active;
    private UUID createdBy;
    private Instant createdAt;
    private UUID updatedBy;
    private Instant updatedAt;

    private PatientChronicDisease(
            UUID id,
            UUID patientId,
            UUID diagnosisCatalogId,
            Integer yearDetected,
            String notes,
            boolean active,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Chronic disease ID cannot be null");
        this.patientId = Guard.require(patientId, "Patient ID");
        this.diagnosisCatalogId = Guard.require(diagnosisCatalogId, "Diagnosis catalog ID");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.yearDetected = validateYearDetected(yearDetected, this.createdAt);
        this.notes = notes != null && !notes.isBlank() ? notes.trim() : null;
        this.active = active;
        this.createdBy = Guard.require(createdBy, "Created by");
        this.updatedBy = updatedBy;
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }

    public static PatientChronicDisease create(
            UUID patientId,
            UUID diagnosisCatalogId,
            Integer yearDetected,
            String notes,
            UUID createdBy,
            Instant createdAt
    ) {
        Instant now = createdAt != null ? createdAt : Instant.now();
        return new PatientChronicDisease(
                UUID.randomUUID(),
                patientId,
                diagnosisCatalogId,
                yearDetected,
                notes,
                true,
                createdBy,
                now,
                null,
                now
        );
    }

    public static PatientChronicDisease restore(
            UUID id,
            UUID patientId,
            UUID diagnosisCatalogId,
            Integer yearDetected,
            String notes,
            boolean active,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        return new PatientChronicDisease(
                id,
                patientId,
                diagnosisCatalogId,
                yearDetected,
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

    private static Integer validateYearDetected(Integer yearDetected, Instant createdAt) {
        if (yearDetected == null) {
            return null;
        }
        int currentYear = createdAt.atZone(CLINIC_ZONE).getYear();
        if (yearDetected < MIN_YEAR || yearDetected > currentYear) {
            throw new ValidationException("Năm phát hiện bệnh không hợp lệ.");
        }
        return yearDetected;
    }
}
