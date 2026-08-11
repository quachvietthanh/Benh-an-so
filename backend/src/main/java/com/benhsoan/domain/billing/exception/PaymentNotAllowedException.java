package com.benhsoan.domain.billing.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotAllowedException extends BillingException {

    public PaymentNotAllowedException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
