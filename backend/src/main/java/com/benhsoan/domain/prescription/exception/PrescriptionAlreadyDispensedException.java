package com.benhsoan.domain.prescription.exception;

import org.springframework.http.HttpStatus;

public class PrescriptionAlreadyDispensedException
        extends PrescriptionException {

    public PrescriptionAlreadyDispensedException() {
        super(
                HttpStatus.CONFLICT,
                "Prescription has already been dispensed."
        );
    }
}
