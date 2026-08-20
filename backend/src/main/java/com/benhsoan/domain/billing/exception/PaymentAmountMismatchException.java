package com.benhsoan.domain.billing.exception;

import java.math.BigDecimal;


public class PaymentAmountMismatchException extends BillingException {

    public PaymentAmountMismatchException(BigDecimal expectedAmount, BigDecimal actualAmount) {
        super(
                "Payment amount must equal the amount due. Expected: "
                        + expectedAmount
                        + ", actual: "
                        + actualAmount
        );
    }
}
