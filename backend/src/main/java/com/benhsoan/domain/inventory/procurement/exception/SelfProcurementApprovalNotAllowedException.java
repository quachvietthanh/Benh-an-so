package com.benhsoan.domain.inventory.procurement.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class SelfProcurementApprovalNotAllowedException extends DomainException {

    public SelfProcurementApprovalNotAllowedException() {
        super(DomainErrorCode.SELF_APPROVAL_NOT_ALLOWED,
                "Người lập phiếu dự trù mua thuốc không được tự phê duyệt hoặc từ chối phiếu của chính mình.");
    }
}
