package com.benhsoan.domain.patient.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class PatientException extends DomainException {

    protected PatientException(String message) {
        super(message);
    }
}
