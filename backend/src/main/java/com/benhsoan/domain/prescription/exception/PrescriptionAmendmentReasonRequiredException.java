package com.benhsoan.domain.prescription.exception;

import org.springframework.http.HttpStatus;

public class PrescriptionAmendmentReasonRequiredException
        extends PrescriptionException {

    public PrescriptionAmendmentReasonRequiredException() {
        super(
                HttpStatus.BAD_REQUEST,
                "Amendment reason is required."
        );
    }
}
