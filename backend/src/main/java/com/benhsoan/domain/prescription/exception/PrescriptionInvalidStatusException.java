package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class PrescriptionInvalidStatusException
        extends PrescriptionException {

    public PrescriptionInvalidStatusException(String message) {
        super(DomainErrorCode.PRESCRIPTION_INVALID_STATUS, message);
    }
}
