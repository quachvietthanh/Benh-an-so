package com.benhsoan.domain.prescription.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class PrescriptionException extends DomainException {

    protected PrescriptionException(
            String message
    ) {
        super(message);
    }
}
