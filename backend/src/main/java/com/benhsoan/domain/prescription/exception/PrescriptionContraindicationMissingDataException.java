package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PrescriptionContraindicationMissingDataException extends PrescriptionException {

    public PrescriptionContraindicationMissingDataException() {
        super(
                DomainErrorCode.CONTRAINDICATION_DATA_MISSING,
                "Required patient information is missing; the contraindication check cannot be completed."
        );
    }
}
