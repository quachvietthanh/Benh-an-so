package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class PatientException extends DomainException {

    protected PatientException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
