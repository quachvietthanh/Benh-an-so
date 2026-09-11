package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.math.BigDecimal;


public class PaymentAmountMismatchException extends BillingException {

    public PaymentAmountMismatchException(BigDecimal expectedAmount, BigDecimal actualAmount) {
        super(DomainErrorCode.PAYMENT_AMOUNT_MISMATCH,
                "Payment amount cannot exceed the amount due. Maximum allowed: "
                        + expectedAmount
                        + ", actual: "
                        + actualAmount
        );
    }
}
