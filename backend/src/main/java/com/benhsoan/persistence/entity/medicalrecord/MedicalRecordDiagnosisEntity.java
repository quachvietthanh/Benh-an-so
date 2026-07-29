package com.benhsoan.persistence.entity.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;

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
@Table(name = "medical_record_diagnoses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordDiagnosisEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(name = "medical_record_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID medicalRecordId;
    @Column(name = "diagnosis_catalog_id", columnDefinition = "BINARY(16)")
    UUID diagnosisCatalogId;
    @Column(name = "diagnosis_code", length = 30)
    String diagnosisCode;
    @Column(name = "diagnosis_name", nullable = false, length = 150)
    String diagnosisName;
    @Enumerated(EnumType.STRING)
    @Column(name = "diagnosis_type", nullable = false, length = 30)
    DiagnosisType diagnosisType;
    @Column(columnDefinition = "TEXT")
    String note;
    @Column(name = "diagnosed_by", nullable = false, columnDefinition = "BINARY(16)")
    UUID diagnosedBy;
    @Column(name = "diagnosed_at", nullable = false)
    Instant diagnosedAt;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "updated_at")
    Instant updatedAt;
}
