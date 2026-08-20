package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class UnauthorizedPrescriptionAmendmentException
        extends PrescriptionException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedPrescriptionAmendmentException() {
        super(DomainErrorCode.UNAUTHORIZED_PRESCRIPTION_AMENDMENT,
                "Only the doctor who prescribed the prescription may amend it."
        );
    }
}
