package com.benhsoan.port.dto.result;

import java.util.UUID;

import com.benhsoan.domain.contraindication.enums.ContraindicationType;

public record ContraindicationMissingDataResult(
        UUID medicineId,
        String medicineName,
        ContraindicationType type,
        String message
) {
}
