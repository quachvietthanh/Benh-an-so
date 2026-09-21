package com.benhsoan.adapter.inbound.rest.response.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountRequestResponse {

    private UUID id;
    private UUID visitId;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String reason;
    private DiscountRequestStatus status;
    private UUID requestedBy;
    private Instant requestedAt;
    private UUID approvedBy;
    private Instant approvedAt;
    private UUID rejectedBy;
    private String rejectionReason;
    private Instant rejectedAt;
    private UUID invoiceId;
}
