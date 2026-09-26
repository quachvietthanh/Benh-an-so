package com.benhsoan.persistence.entity.prescription;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prescription_allergy_warning_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionAllergyWarningLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "prescription_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionId;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "allergy_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID allergyId;

    @Column(name = "medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Column(name = "active_ingredient", nullable = false, length = 255)
    private String activeIngredient;

    @Column(name = "allergen_name", nullable = false, length = 255)
    private String allergenName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private AllergySeverity severity;

    @Column(name = "reaction", length = 255)
    private String reaction;

    @Column(name = "override_reason", nullable = false, columnDefinition = "TEXT")
    private String overrideReason;

    @Column(name = "handled_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID handledBy;

    @Column(name = "handled_at", nullable = false)
    private Instant handledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
