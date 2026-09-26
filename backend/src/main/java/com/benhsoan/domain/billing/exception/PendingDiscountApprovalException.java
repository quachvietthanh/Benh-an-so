package com.benhsoan.domain.billing.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PendingDiscountApprovalException extends BillingException {

    public PendingDiscountApprovalException(UUID visitId) {
        super(DomainErrorCode.PENDING_DISCOUNT_APPROVAL, "Lượt khám đang có đề nghị giảm giá chờ duyệt: " + visitId);
    }
}
