package com.benhsoan.port.dto.result;

import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;

public record DrugInteractionWarningResult(

        UUID drugIdA,

        UUID drugIdB,

        InteractionSeverity severity,

        String description,

        String clinicalRecommendation

) {
}
