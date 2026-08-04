package com.benhsoan.domain.prescription.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class PrescriptionException extends DomainException {

    protected PrescriptionException(
            HttpStatus status,
            String message
    ) {
        super(status, message);
    }
}
