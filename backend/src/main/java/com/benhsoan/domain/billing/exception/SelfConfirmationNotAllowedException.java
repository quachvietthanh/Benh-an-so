package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class SelfConfirmationNotAllowedException extends DomainException {

    public SelfConfirmationNotAllowedException() {
        super(
                DomainErrorCode.CASHIER_SHIFT_SELF_CONFIRMATION_NOT_ALLOWED,
                "Người chốt ca không được tự phê duyệt phiếu lệch quỹ của chính mình."
        );
    }
}
