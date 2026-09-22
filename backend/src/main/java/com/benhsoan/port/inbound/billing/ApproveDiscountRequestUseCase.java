package com.benhsoan.port.inbound.billing;

import java.util.UUID;

import com.benhsoan.port.dto.result.DiscountRequestResult;

public interface ApproveDiscountRequestUseCase {

    DiscountRequestResult approve(UUID discountRequestId);
}
