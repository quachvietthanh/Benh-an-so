package com.benhsoan.port.dto.result;

import java.util.UUID;

import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;

public record ContraindicationWarningResult(
        UUID patientId,
        UUID ruleId,
        UUID medicineId,
        String medicineName,
        ContraindicationType type,
        ContraindicationSeverity severity,
        String message,
        String recommendation
) {
}
