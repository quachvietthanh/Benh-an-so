package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;

public record DrugInteractionWarningResponse(

        UUID drugIdA,

        UUID drugIdB,

        InteractionSeverity severity,

        String description,

        String clinicalRecommendation

) {
}
