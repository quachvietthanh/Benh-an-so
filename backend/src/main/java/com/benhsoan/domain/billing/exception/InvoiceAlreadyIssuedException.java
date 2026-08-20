package com.benhsoan.domain.billing.exception;

import java.util.UUID;


public class InvoiceAlreadyIssuedException extends BillingException {

    public InvoiceAlreadyIssuedException(UUID visitId) {
        super("An invoice already exists for visit: " + visitId);
    }
}
