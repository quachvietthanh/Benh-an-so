package com.benhsoan.domain.prescription.exception;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.druginteraction.enums.InteractionSeverity;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.Getter;

@Getter
public class PrescriptionInteractionConfirmationRequiredException
        extends ValidationException {

    private final List<InteractionWarning> warnings;

    public PrescriptionInteractionConfirmationRequiredException(
            List<InteractionWarning> warnings
    ) {
        super("All detected drug interactions must be confirmed with an override reason.");
        this.warnings = List.copyOf(warnings);
    }

    public record InteractionWarning(
            UUID drugInteractionId,
            UUID firstMedicineId,
            UUID secondMedicineId,
            InteractionSeverity severity,
            String description,
            String recommendation
    ) {
    }
}
