package com.benhsoan.domain.billing.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class PaymentAlreadyExistsException extends BillingException {

    public PaymentAlreadyExistsException(UUID visitId) {
        super(HttpStatus.CONFLICT, "A payment already exists for visit: " + visitId);
    }
}
