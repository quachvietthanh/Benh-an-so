package com.benhsoan.domain.billing.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends BillingException {

    public PaymentNotFoundException(UUID paymentId) {
        super(HttpStatus.NOT_FOUND, "Payment not found: " + paymentId);
    }

    public PaymentNotFoundException(UUID visitId, boolean byVisitId) {
        super(HttpStatus.NOT_FOUND, "Payment not found for visit: " + visitId);
    }
}
