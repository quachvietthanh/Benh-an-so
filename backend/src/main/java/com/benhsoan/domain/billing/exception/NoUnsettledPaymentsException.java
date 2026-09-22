package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class NoUnsettledPaymentsException extends BillingException {

    public NoUnsettledPaymentsException() {
        super(DomainErrorCode.NO_UNSETTLED_PAYMENTS, "Ca làm việc chưa có khoản thu nào để chốt ca.");
    }
}
