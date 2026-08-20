package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class InvoiceNotFoundException extends BillingException {

    public InvoiceNotFoundException(UUID invoiceId) {
        super(DomainErrorCode.INVOICE_NOT_FOUND, "Invoice not found: " + invoiceId);
    }

    public InvoiceNotFoundException(UUID paymentId, boolean byPaymentId) {
        super(DomainErrorCode.INVOICE_NOT_FOUND, "Original invoice not found for payment: " + paymentId);
    }
}
