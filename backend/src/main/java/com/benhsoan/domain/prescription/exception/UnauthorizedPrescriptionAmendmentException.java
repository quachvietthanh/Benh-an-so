package com.benhsoan.domain.prescription.exception;


public class UnauthorizedPrescriptionAmendmentException
        extends PrescriptionException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedPrescriptionAmendmentException() {
        super(
                "Only the doctor who prescribed the prescription may amend it."
        );
    }
}
