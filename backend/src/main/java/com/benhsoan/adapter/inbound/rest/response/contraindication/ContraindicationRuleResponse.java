package com.benhsoan.adapter.inbound.rest.response.contraindication;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;

public record ContraindicationRuleResponse(
        UUID id,
        UUID medicineId,
        String medicineName,
        String activeIngredient,
        ContraindicationType type,
        Integer minAgeYears,
        Integer maxAgeYears,
        UUID diagnosisCatalogId,
        String diagnosisName,
        ContraindicationSeverity severity,
        String message,
        String recommendation,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
