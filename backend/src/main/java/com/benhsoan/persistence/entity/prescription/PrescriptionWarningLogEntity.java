package com.benhsoan.persistence.entity.prescription;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.prescription.enums.WarningAction;

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
@Table(name = "prescription_warning_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionWarningLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "prescription_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionId;

    @Column(name = "rule_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ruleId;

    @Column(name = "first_medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID firstMedicineId;

    @Column(name = "second_medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID secondMedicineId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private InteractionSeverity severity;

    @Column(name = "warning_message", nullable = false, columnDefinition = "TEXT")
    private String warningMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private WarningAction action;

    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @Column(name = "handled_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID handledBy;

    @Column(name = "handled_at", nullable = false)
    private Instant handledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
