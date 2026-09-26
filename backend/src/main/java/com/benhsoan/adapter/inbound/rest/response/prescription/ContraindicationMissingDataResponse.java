package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.UUID;

import com.benhsoan.domain.contraindication.enums.ContraindicationType;

public record ContraindicationMissingDataResponse(
        UUID medicineId,
        String medicineName,
        ContraindicationType type,
        String message
) {
}
