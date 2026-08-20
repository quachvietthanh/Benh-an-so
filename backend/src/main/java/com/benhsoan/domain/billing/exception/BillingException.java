package com.benhsoan.domain.billing.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class BillingException extends DomainException {

    protected BillingException(String message) {
        super(message);
    }
}
