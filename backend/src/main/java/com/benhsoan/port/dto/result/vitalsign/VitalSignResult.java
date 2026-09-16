package com.benhsoan.port.dto.result.vitalsign;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.vitalsign.enums.VitalSignAbnormalFlag;

public record VitalSignResult(
        UUID id,
        UUID visitId,
        UUID patientId,
        UUID medicalRecordId,
        Integer pulse,
        Integer bloodPressureSystolic,
        Integer bloodPressureDiastolic,
        BigDecimal temperature,
        Integer respiratoryRate,
        BigDecimal weight,
        BigDecimal height,
        BigDecimal bmi,
        Integer spo2,
        boolean abnormal,
        List<VitalSignAbnormalFlag> abnormalFlags,
        String note,
        UUID recordedBy,
        Instant recordedAt,
        UUID updatedBy,
        Instant updatedAt
) {
}
