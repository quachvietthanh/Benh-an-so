package com.benhsoan.domain.billing.exception;

import java.util.UUID;


public class InvoiceNotFoundException extends BillingException {

    public InvoiceNotFoundException(UUID invoiceId) {
        super("Invoice not found: " + invoiceId);
    }

    public InvoiceNotFoundException(UUID paymentId, boolean byPaymentId) {
        super("Original invoice not found for payment: " + paymentId);
    }
}
