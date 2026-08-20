package com.benhsoan.domain.billing.exception;


public class PaymentNotAllowedException extends BillingException {

    public PaymentNotAllowedException(String message) {
        super(message);
    }
}
