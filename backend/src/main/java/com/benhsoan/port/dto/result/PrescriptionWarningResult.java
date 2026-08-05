package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.prescription.enums.WarningAction;

public record PrescriptionWarningResult(

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
