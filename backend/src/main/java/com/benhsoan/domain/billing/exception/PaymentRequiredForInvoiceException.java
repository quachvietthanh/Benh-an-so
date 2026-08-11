package com.benhsoan.domain.billing.exception;

import org.springframework.http.HttpStatus;

public class PaymentRequiredForInvoiceException extends BillingException {

    public PaymentRequiredForInvoiceException() {
        super(HttpStatus.CONFLICT, "A recorded payment is required before issuing an invoice.");
    }
}
