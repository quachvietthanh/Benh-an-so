package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class BillingException extends DomainException {

    protected BillingException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
