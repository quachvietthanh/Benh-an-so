package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class PaymentAlreadyExistsException extends BillingException {

    public PaymentAlreadyExistsException(UUID visitId) {
        super(DomainErrorCode.PAYMENT_ALREADY_EXISTS, "A payment already exists for visit: " + visitId);
    }
}
