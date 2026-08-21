package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class PaymentRequiredForInvoiceException extends BillingException {

    public PaymentRequiredForInvoiceException() {
        super(DomainErrorCode.PAYMENT_REQUIRED_FOR_INVOICE, "A recorded payment is required before issuing an invoice.");
    }
}
