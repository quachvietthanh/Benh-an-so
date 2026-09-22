package com.benhsoan.domain.billing.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class SelfApprovalNotAllowedException extends BillingException {

    public SelfApprovalNotAllowedException() {
        super(DomainErrorCode.SELF_APPROVAL_NOT_ALLOWED, "Người đề nghị giảm giá không được tự phê duyệt đề nghị của chính mình.");
    }
}
