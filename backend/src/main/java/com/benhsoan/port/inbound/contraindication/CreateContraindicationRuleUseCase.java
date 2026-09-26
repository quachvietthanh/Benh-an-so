package com.benhsoan.port.inbound.contraindication;

import java.util.UUID;

import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;

public interface CreateContraindicationRuleUseCase {
    ContraindicationRule create(
            UUID medicineId,
            String activeIngredient,
            ContraindicationType type,
            Integer minAgeYears,
            Integer maxAgeYears,
            UUID diagnosisCatalogId,
            ContraindicationSeverity severity,
            String message,
            String recommendation
    );
}
