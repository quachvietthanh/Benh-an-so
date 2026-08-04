package com.benhsoan.domain.prescription.exception;

import org.springframework.http.HttpStatus;

public class PrescriptionOverrideReasonRequiredException
        extends PrescriptionException {

    public PrescriptionOverrideReasonRequiredException() {
        super(
                HttpStatus.BAD_REQUEST,
                "Override reason is required when a drug interaction "
                + "warning is overridden."
        );
    }
}
