package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;

public record DiscountRequestResult(
        UUID id,
        UUID visitId,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String reason,
        DiscountRequestStatus status,
        UUID requestedBy,
        Instant requestedAt,
        UUID approvedBy,
        Instant approvedAt,
        UUID rejectedBy,
        String rejectionReason,
        Instant rejectedAt,
        UUID invoiceId
) {
}
