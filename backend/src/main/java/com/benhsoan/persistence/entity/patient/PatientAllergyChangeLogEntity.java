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
@Table(name = "patient_allergy_change_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientAllergyChangeLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "allergy_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID allergyId;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "before_data", columnDefinition = "JSON")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "JSON")
    private String afterData;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "changed_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
