package com.benhsoan.adapter.inbound.rest.request.contraindication;

import java.util.UUID;

import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateContraindicationRuleRequest(
        UUID medicineId,
        String activeIngredient,
        @NotNull(message = "Contraindication type is required")
        ContraindicationType type,
        Integer minAgeYears,
        Integer maxAgeYears,
        UUID diagnosisCatalogId,
        @NotNull(message = "Severity is required")
        ContraindicationSeverity severity,
        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message must not exceed 500 characters")
        String message,
        @Size(max = 500, message = "Recommendation must not exceed 500 characters")
        String recommendation
) {}
