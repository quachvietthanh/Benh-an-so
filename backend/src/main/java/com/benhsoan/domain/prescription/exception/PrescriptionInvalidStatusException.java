package com.benhsoan.domain.prescription.exception;

import org.springframework.http.HttpStatus;

public class PrescriptionInvalidStatusException
        extends PrescriptionException {

    public PrescriptionInvalidStatusException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
