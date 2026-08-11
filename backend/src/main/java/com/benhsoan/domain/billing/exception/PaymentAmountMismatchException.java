package com.benhsoan.domain.billing.exception;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;

public class PaymentAmountMismatchException extends BillingException {

    public PaymentAmountMismatchException(BigDecimal expectedAmount, BigDecimal actualAmount) {
        super(
                HttpStatus.BAD_REQUEST,
                "Payment amount must equal the amount due. Expected: "
                        + expectedAmount
                        + ", actual: "
                        + actualAmount
        );
    }
}
