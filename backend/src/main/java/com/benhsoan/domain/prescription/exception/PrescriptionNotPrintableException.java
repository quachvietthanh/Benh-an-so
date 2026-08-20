package com.benhsoan.domain.prescription.exception;

import org.springframework.http.HttpStatus;

public class PrescriptionNotPrintableException extends PrescriptionException {

    public PrescriptionNotPrintableException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
