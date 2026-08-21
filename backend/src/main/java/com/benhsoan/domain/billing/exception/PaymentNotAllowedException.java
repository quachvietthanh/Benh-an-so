package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class PaymentNotAllowedException extends BillingException {

    public PaymentNotAllowedException(String message) {
        super(DomainErrorCode.PAYMENT_NOT_ALLOWED, message);
    }
}
