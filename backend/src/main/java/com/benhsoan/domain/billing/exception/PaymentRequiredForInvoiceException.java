package com.benhsoan.domain.billing.exception;


public class PaymentRequiredForInvoiceException extends BillingException {

    public PaymentRequiredForInvoiceException() {
        super("A recorded payment is required before issuing an invoice.");
    }
}
