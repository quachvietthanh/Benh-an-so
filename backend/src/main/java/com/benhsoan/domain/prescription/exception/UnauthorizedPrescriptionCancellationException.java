package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class UnauthorizedPrescriptionCancellationException
        extends PrescriptionException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedPrescriptionCancellationException() {
        super(DomainErrorCode.UNAUTHORIZED_PRESCRIPTION_CANCELLATION,
                "Only the doctor who prescribed the prescription may cancel it."
        );
    }
}
