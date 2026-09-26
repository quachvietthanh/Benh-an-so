package com.benhsoan.application.ucservice.billing;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.DiscountRequest;
import com.benhsoan.port.dto.result.DiscountRequestResult;

@Component
public class DiscountRequestResultMapper {

    public DiscountRequestResult toResult(DiscountRequest request) {
        if (request == null) {
            return null;
        }

        return new DiscountRequestResult(
                request.getId(),
                request.getVisitId(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getOriginalAmount(),
                request.getDiscountAmount(),
                request.getFinalAmount(),
                request.getReason(),
                request.getStatus(),
                request.getRequestedBy(),
                request.getRequestedAt(),
                request.getApprovedBy(),
                request.getApprovedAt(),
                request.getRejectedBy(),
                request.getRejectionReason(),
                request.getRejectedAt(),
                request.getInvoiceId()
        );
    }
}
