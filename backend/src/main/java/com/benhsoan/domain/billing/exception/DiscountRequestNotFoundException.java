package com.benhsoan.domain.billing.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class DiscountRequestNotFoundException extends BillingException {

    public DiscountRequestNotFoundException(UUID discountRequestId) {
        super(DomainErrorCode.DISCOUNT_REQUEST_NOT_FOUND, "Không tìm thấy yêu cầu giảm giá: " + discountRequestId);
    }
}
