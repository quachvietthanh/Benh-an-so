package com.benhsoan.domain.prescription.exception;

import org.springframework.http.HttpStatus;

public class PrescriptionClinicalContextConflictException
        extends PrescriptionException {

    public PrescriptionClinicalContextConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
