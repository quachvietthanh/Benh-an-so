package com.benhsoan.domain.billing.exception;

import org.springframework.http.HttpStatus;

public class InvoiceUnauthorizedAdjustmentException extends BillingException {

    public InvoiceUnauthorizedAdjustmentException() {
        super(HttpStatus.FORBIDDEN, "Only clinic managers can adjust invoices.");
    }
}
