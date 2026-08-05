package com.benhsoan.domain.prescription.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedPrescriptionAmendmentException
        extends PrescriptionException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedPrescriptionAmendmentException() {
        super(
                HttpStatus.FORBIDDEN,
                "Only the doctor who prescribed the prescription may amend it."
        );
    }
}
