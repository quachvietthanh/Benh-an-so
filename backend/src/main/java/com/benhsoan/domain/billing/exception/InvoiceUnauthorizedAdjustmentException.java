package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class InvoiceUnauthorizedAdjustmentException extends BillingException {

    public InvoiceUnauthorizedAdjustmentException() {
        super(DomainErrorCode.INVOICE_UNAUTHORIZED_ADJUSTMENT, "Only clinic managers can adjust invoices.");
    }
}
