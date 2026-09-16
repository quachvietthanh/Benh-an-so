package com.benhsoan.persistence.entity.patient;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patient_chronic_diseases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientChronicDiseaseEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "diagnosis_catalog_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID diagnosisCatalogId;

    @Column(name = "year_detected")
    private Integer yearDetected;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by", columnDefinition = "BINARY(16)")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
