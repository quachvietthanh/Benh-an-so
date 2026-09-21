package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class CashierShiftNoteRequiredException extends BillingException {

    public CashierShiftNoteRequiredException() {
        super(
                DomainErrorCode.CASHIER_SHIFT_NOTE_REQUIRED,
                "Bắt buộc nhập ghi chú giải trình khi số tiền thực tế có chênh lệch với hệ thống."
        );
    }
}
