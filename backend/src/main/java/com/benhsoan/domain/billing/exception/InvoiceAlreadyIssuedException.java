package com.benhsoan.domain.billing.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class InvoiceAlreadyIssuedException extends BillingException {

    public InvoiceAlreadyIssuedException(UUID visitId) {
        super(HttpStatus.CONFLICT, "An invoice already exists for visit: " + visitId);
    }
}
