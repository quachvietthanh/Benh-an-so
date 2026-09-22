package com.benhsoan.domain.billing.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class CashierShiftAlreadyConfirmedException extends BillingException {

    public CashierShiftAlreadyConfirmedException(UUID shiftId) {
        super(
                DomainErrorCode.CASHIER_SHIFT_ALREADY_CONFIRMED,
                "Phiếu chốt ca thu ngân đã được xác nhận trước đó: " + shiftId
        );
    }
}
