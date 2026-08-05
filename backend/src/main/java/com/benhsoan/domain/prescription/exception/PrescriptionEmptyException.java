package com.benhsoan.domain.prescription.exception;

import org.springframework.http.HttpStatus;

public class PrescriptionEmptyException extends PrescriptionException {

    public PrescriptionEmptyException() {
        super(
                HttpStatus.BAD_REQUEST,
                "Prescription must contain at least one medicine."
        );
    }
}
