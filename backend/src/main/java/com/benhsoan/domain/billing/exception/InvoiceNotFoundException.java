package com.benhsoan.domain.billing.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class InvoiceNotFoundException extends BillingException {

    public InvoiceNotFoundException(UUID invoiceId) {
        super(HttpStatus.NOT_FOUND, "Invoice not found: " + invoiceId);
    }
}
