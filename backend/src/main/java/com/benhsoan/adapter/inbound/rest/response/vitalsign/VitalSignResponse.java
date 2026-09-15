package com.benhsoan.adapter.inbound.rest.response.vitalsign;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.vitalsign.enums.VitalSignAbnormalFlag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSignResponse {

    private UUID id;
    private UUID visitId;
    private UUID patientId;
    private UUID medicalRecordId;

    private Integer pulse;
    private Integer bloodPressureSystolic;
    private Integer bloodPressureDiastolic;
    private BigDecimal temperature;
    private Integer respiratoryRate;
    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal bmi;
    private Integer spo2;

    private boolean abnormal;
    private List<VitalSignAbnormalFlag> abnormalFlags;
    private String note;

    private UUID recordedBy;
    private Instant recordedAt;
    private UUID updatedBy;
    private Instant updatedAt;
}
