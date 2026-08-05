package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.prescription.enums.WarningAction;

import lombok.Builder;

@Builder
public record PrescriptionWarningResponse(

        UUID id,

        UUID ruleId,

        UUID firstMedicineId,

        UUID secondMedicineId,

        InteractionSeverity severity,

        String warningMessage,

        WarningAction action,

        String overrideReason,

        UUID handledBy,

        Instant handledAt

) {
}
