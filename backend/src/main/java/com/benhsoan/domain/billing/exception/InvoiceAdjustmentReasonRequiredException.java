package com.benhsoan.domain.billing.exception;

import org.springframework.http.HttpStatus;

public class InvoiceAdjustmentReasonRequiredException extends BillingException {

    public InvoiceAdjustmentReasonRequiredException() {
        super(HttpStatus.BAD_REQUEST, "Adjustment reason is required.");
    }
}
