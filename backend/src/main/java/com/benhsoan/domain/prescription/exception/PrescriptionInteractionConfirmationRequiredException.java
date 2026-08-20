package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import lombok.Getter;

@Getter
public class PrescriptionInteractionConfirmationRequiredException
        extends PrescriptionException {

    private final List<InteractionWarning> warnings;

    public PrescriptionInteractionConfirmationRequiredException(
            List<InteractionWarning> warnings
    ) {
        super(DomainErrorCode.INTERACTION_CONFIRMATION_REQUIRED,
                "All detected drug interactions must be confirmed with an override reason."
        );
        this.warnings = List.copyOf(warnings);
    }

    public record InteractionWarning(
            UUID ruleId,
            UUID firstMedicineId,
            UUID secondMedicineId,
            InteractionSeverity severity,
            String description,
            String recommendation
    ) {
    }
}
