package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class PrescriptionNotPrintableException extends PrescriptionException {

    public PrescriptionNotPrintableException(String message) {
        super(DomainErrorCode.PRESCRIPTION_NOT_PRINTABLE, message);
    }
}
