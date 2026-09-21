package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class InvalidDiscountStateException extends BillingException {

    public InvalidDiscountStateException(String message) {
        super(DomainErrorCode.INVALID_DISCOUNT_STATE, message);
    }
}
