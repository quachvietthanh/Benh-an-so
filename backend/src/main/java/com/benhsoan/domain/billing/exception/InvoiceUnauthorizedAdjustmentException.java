package com.benhsoan.domain.billing.exception;


public class InvoiceUnauthorizedAdjustmentException extends BillingException {

    public InvoiceUnauthorizedAdjustmentException() {
        super("Only clinic managers can adjust invoices.");
    }
}
