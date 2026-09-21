package com.benhsoan.port.dto.command.billing;

import java.math.BigDecimal;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.DiscountType;

import lombok.Builder;

@Builder
public record CreateDiscountRequestCommand(
        UUID visitId,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal originalAmount,
        String reason
) {
}
