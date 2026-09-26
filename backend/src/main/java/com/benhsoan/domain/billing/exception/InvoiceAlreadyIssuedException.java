package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class InvoiceAlreadyIssuedException extends BillingException {

    public InvoiceAlreadyIssuedException(UUID visitId) {
        super(DomainErrorCode.INVOICE_ALREADY_ISSUED, "An invoice already exists for visit: " + visitId);
    }
}
