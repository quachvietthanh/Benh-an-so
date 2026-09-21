package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class DiscountExceedsTotalException extends BillingException {

    public DiscountExceedsTotalException(String message) {
        super(DomainErrorCode.DISCOUNT_EXCEEDS_TOTAL, message);
    }
}
