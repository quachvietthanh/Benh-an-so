package com.benhsoan.persistence.entity.prescription;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;

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
@Table(name = "prescription_contraindication_warning_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionContraindicationWarningLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "prescription_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionId;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "rule_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ruleId;

    @Column(name = "medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "contraindication_type", nullable = false, length = 30)
    private ContraindicationType contraindicationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private ContraindicationSeverity severity;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "recommendation", length = 500)
    private String recommendation;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "handled_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID handledBy;

    @Column(name = "handled_at", nullable = false)
    private Instant handledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
