package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PrescriptionContraindicationConfirmationRequiredException extends PrescriptionException {

    public PrescriptionContraindicationConfirmationRequiredException() {
        super(
                DomainErrorCode.CONTRAINDICATION_CONFIRMATION_REQUIRED,
                "Contraindication warnings require an override reason before the prescription can be completed."
        );
    }
}
