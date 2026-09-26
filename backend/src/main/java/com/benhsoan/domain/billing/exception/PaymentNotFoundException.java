package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class PaymentNotFoundException extends BillingException {

    public PaymentNotFoundException(UUID paymentId) {
        super(DomainErrorCode.PAYMENT_NOT_FOUND, "Payment not found: " + paymentId);
    }

    public PaymentNotFoundException(UUID visitId, boolean byVisitId) {
        super(DomainErrorCode.PAYMENT_NOT_FOUND, "Payment not found for visit: " + visitId);
    }
}
