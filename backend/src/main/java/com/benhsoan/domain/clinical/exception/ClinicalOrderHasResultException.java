package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class ClinicalOrderHasResultException extends ClinicalOrderException {

    public ClinicalOrderHasResultException() {
        super(DomainErrorCode.CLINICAL_ORDER_HAS_RESULT,
                "Không thể hủy chỉ định vì kết quả cận lâm sàng đã được nhập và gắn với lượt khám (QTN-13).");
    }

    public ClinicalOrderHasResultException(String message) {
        super(DomainErrorCode.CLINICAL_ORDER_HAS_RESULT, message);
    }
}
