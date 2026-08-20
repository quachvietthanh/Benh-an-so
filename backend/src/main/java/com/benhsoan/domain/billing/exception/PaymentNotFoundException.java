package com.benhsoan.domain.billing.exception;

import java.util.UUID;


public class PaymentNotFoundException extends BillingException {

    public PaymentNotFoundException(UUID paymentId) {
        super("Payment not found: " + paymentId);
    }

    public PaymentNotFoundException(UUID visitId, boolean byVisitId) {
        super("Payment not found for visit: " + visitId);
    }
}
