package com.benhsoan.persistence.entity.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;

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
@Table(name = "medical_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(name = "visit_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID visitId;
    @Column(columnDefinition = "TEXT")
    String chiefComplaint;
    @Column(columnDefinition = "TEXT")
    String symptoms;
    @Column(name = "medical_history", columnDefinition = "TEXT")
    String medicalHistory;
    @Column(name = "physical_examination", columnDefinition = "TEXT")
    String physicalExamination;
    @Column(name = "clinical_progress", columnDefinition = "TEXT")
    String clinicalProgress;
    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    String treatmentPlan;
    @Column(name = "doctor_instructions", columnDefinition = "TEXT")
    String doctorInstructions;
    @Column(columnDefinition = "TEXT")
    String conclusion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    MedicalRecordStatus status;
    @Column(name = "locked_at")
    Instant lockedAt;
    @Column(name = "locked_by", columnDefinition = "BINARY(16)")
    UUID lockedBy;
    @Column(name = "created_by", nullable = false, columnDefinition = "BINARY(16)")
    UUID createdBy;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "updated_by", columnDefinition = "BINARY(16)")
    UUID updatedBy;
    @Column(name = "updated_at")
    Instant updatedAt;
}
