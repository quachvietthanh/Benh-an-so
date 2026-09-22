package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when a controlled medicine (QTN-39, NCL-06-CN-014) is prescribed or
 * dispensed without the required additional confirmation.
 */
public class ControlledMedicineConfirmationRequiredException
        extends PrescriptionException {

    public ControlledMedicineConfirmationRequiredException() {
        super(
                DomainErrorCode.CONTROLLED_MEDICINE_CONFIRMATION_REQUIRED,
                "Controlled medicines require an additional confirmation before prescribing or dispensing."
        );
    }
}
