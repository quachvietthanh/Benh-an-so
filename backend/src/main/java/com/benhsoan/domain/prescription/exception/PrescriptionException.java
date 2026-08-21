package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class PrescriptionException extends DomainException {

    protected PrescriptionException(
            DomainErrorCode code,
            String message
    ) {
        super(code, message);
    }
}
