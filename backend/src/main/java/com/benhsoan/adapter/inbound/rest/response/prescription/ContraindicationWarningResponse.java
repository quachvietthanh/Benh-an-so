package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.UUID;

import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;

public record ContraindicationWarningResponse(
        UUID ruleId,
        UUID medicineId,
        String medicineName,
        ContraindicationType type,
        ContraindicationSeverity severity,
        String message,
        String recommendation
) {
}
