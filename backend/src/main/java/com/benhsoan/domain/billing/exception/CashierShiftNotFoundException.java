package com.benhsoan.domain.billing.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class CashierShiftNotFoundException extends BillingException {

    public CashierShiftNotFoundException(UUID shiftId) {
        super(
                DomainErrorCode.CASHIER_SHIFT_NOT_FOUND,
                "Không tìm thấy phiếu chốt ca thu ngân với mã định danh: " + shiftId
        );
    }
}
