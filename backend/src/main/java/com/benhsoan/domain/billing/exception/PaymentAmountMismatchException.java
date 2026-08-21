package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.math.BigDecimal;


public class PaymentAmountMismatchException extends BillingException {

    public PaymentAmountMismatchException(BigDecimal expectedAmount, BigDecimal actualAmount) {
        super(DomainErrorCode.PAYMENT_AMOUNT_MISMATCH,
                "Payment amount must equal the amount due. Expected: "
                        + expectedAmount
                        + ", actual: "
                        + actualAmount
        );
    }
}
