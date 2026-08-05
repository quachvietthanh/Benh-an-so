package com.benhsoan.domain.prescription.exception;

import org.springframework.http.HttpStatus;

public class PrescriptionAlreadyCancelledException
        extends PrescriptionException {

    public PrescriptionAlreadyCancelledException() {
        super(
                HttpStatus.CONFLICT,
                "Prescription has already been cancelled."
        );
    }
}
