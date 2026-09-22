package com.benhsoan.port.outbound.repository.billing;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;

public record DiscountRequestSearchCriteria(
        UUID visitId,
        DiscountRequestStatus status,
        DiscountType discountType,
        UUID requestedBy,
        UUID approvedBy,
        Instant requestedFrom,
        Instant requestedTo
) {
}
