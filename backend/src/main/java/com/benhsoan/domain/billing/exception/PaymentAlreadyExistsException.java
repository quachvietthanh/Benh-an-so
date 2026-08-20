package com.benhsoan.domain.billing.exception;

import java.util.UUID;


public class PaymentAlreadyExistsException extends BillingException {

    public PaymentAlreadyExistsException(UUID visitId) {
        super("A payment already exists for visit: " + visitId);
    }
}
