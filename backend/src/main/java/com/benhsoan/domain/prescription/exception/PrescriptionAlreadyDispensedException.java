package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class PrescriptionAlreadyDispensedException
        extends PrescriptionException {

    public PrescriptionAlreadyDispensedException() {
        super(DomainErrorCode.PRESCRIPTION_ALREADY_DISPENSED,
                "Prescription has already been dispensed and cannot be cancelled. Please use medicine return function."
        );
    }

    public PrescriptionAlreadyDispensedException(String message) {
        super(DomainErrorCode.PRESCRIPTION_ALREADY_DISPENSED, message);
    }
}
