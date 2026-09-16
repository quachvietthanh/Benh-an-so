package com.benhsoan.persistence.entity.vitalsign;

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
@Table(name = "vital_signs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSignEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "medical_record_id")
    private UUID medicalRecordId;

    @Column(name = "pulse")
    private Integer pulse;

    @Column(name = "blood_pressure_systolic")
    private Integer bloodPressureSystolic;

    @Column(name = "blood_pressure_diastolic")
    private Integer bloodPressureDiastolic;

    @Column(name = "temperature", precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "height", precision = 5, scale = 2)
    private BigDecimal height;

    @Column(name = "bmi", precision = 5, scale = 1)
    private BigDecimal bmi;

    @Column(name = "spo2")
    private Integer spo2;

    @Column(name = "is_abnormal", nullable = false)
    private boolean abnormal;

    @Column(name = "abnormal_flags", length = 500)
    private String abnormalFlags;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "recorded_by", nullable = false)
    private UUID recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
