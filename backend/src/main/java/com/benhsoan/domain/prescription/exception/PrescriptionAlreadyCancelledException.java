package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class PrescriptionAlreadyCancelledException
        extends PrescriptionException {

    public PrescriptionAlreadyCancelledException() {
        super(DomainErrorCode.PRESCRIPTION_ALREADY_CANCELLED,
                "Prescription has already been cancelled."
        );
    }
}
