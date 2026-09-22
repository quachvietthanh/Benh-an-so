package com.benhsoan.domain.billing.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class DiscountAlreadyExistsException extends BillingException {

    public DiscountAlreadyExistsException(UUID visitId) {
        super(DomainErrorCode.DISCOUNT_ALREADY_EXISTS, "Lượt khám đã có đề nghị giảm giá đang chờ xử lý hoặc đã duyệt: " + visitId);
    }
}
