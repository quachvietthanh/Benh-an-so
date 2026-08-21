package com.benhsoan.domain.shared.exception;

public class ValidationException extends DomainException {

    public ValidationException(String message) {
        this(DomainErrorCode.VALIDATION_FAILED, message);
    }

    protected ValidationException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
