package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.math.BigDecimal;


public class PaymentAmountMismatchException extends BillingException {

    public PaymentAmountMismatchException(BigDecimal expectedAmount, BigDecimal actualAmount) {
        super(DomainErrorCode.PAYMENT_AMOUNT_MISMATCH,
                buildMessage(expectedAmount, actualAmount)
        );
    }

    private static String buildMessage(BigDecimal expectedAmount, BigDecimal actualAmount) {
        if (actualAmount != null && expectedAmount != null && actualAmount.compareTo(expectedAmount) < 0) {
            BigDecimal deficit = expectedAmount.subtract(actualAmount);
            return "Payment amount must equal the amount due. Expected: "
                    + expectedAmount
                    + ", actual: "
                    + actualAmount
                    + ", missing: "
                    + deficit;
        }
        return "Payment amount must equal the amount due. Expected: "
                + expectedAmount
                + ", actual: "
                + actualAmount;
    }
}
