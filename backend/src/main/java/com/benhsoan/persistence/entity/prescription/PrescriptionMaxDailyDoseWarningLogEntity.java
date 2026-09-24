package com.benhsoan.persistence.entity.prescription;

import java.math.BigDecimal;
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
@Table(name = "prescription_max_daily_dose_warning_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionMaxDailyDoseWarningLogEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "prescription_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID prescriptionId;

    @Column(name = "patient_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID patientId;

    @Column(name = "active_ingredient", nullable = false, length = 255)
    private String activeIngredient;

    @Column(name = "total_daily_dose_mg", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalDailyDoseMg;

    @Column(name = "max_daily_dose_mg", nullable = false, precision = 12, scale = 3)
    private BigDecimal maxDailyDoseMg;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "handled_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID handledBy;

    @Column(name = "handled_at", nullable = false)
    private Instant handledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
