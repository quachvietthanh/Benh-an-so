package com.benhsoan.domain.prescription.exception;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PrescriptionAllergyConfirmationRequiredException extends PrescriptionException {

    private final List<AllergyWarning> warnings;

    public PrescriptionAllergyConfirmationRequiredException(List<AllergyWarning> warnings) {
        super(
                DomainErrorCode.ALLERGY_CONFIRMATION_REQUIRED,
                "All detected medication allergies must be confirmed with an override reason."
        );
        this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public List<AllergyWarning> getWarnings() {
        return warnings;
    }

    public record AllergyWarning(
            UUID allergyId,
            UUID patientId,
            UUID medicineId,
            String medicineName,
            String activeIngredient,
            String allergenName,
            AllergySeverity severity,
            String reaction
    ) {
    }
}
