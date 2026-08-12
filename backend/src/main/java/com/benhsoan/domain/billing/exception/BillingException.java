package com.benhsoan.domain.billing.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class BillingException extends DomainException {

    protected BillingException(HttpStatus status, String message) {
        super(status, message);
    }
}
