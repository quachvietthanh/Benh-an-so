package com.benhsoan.persistence.mapper.billing;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.DiscountRequest;
import com.benhsoan.persistence.entity.billing.DiscountRequestEntity;

@Component
public class DiscountRequestPersistenceMapper {

    public DiscountRequest toDomain(DiscountRequestEntity entity) {
        if (entity == null) {
            return null;
        }

        return DiscountRequest.restore(
                entity.getId(),
                entity.getVisitId(),
                entity.getDiscountType(),
                entity.getDiscountValue(),
                entity.getOriginalAmount(),
                entity.getDiscountAmount(),
                entity.getFinalAmount(),
                entity.getReason(),
                entity.getStatus(),
                entity.getRequestedBy(),
                entity.getRequestedAt(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectedBy(),
                entity.getRejectionReason(),
                entity.getRejectedAt(),
                entity.getInvoiceId()
        );
    }

    public DiscountRequestEntity toEntity(DiscountRequest domain) {
        if (domain == null) {
            return null;
        }

        return DiscountRequestEntity.builder()
                .id(domain.getId())
                .visitId(domain.getVisitId())
                .discountType(domain.getDiscountType())
                .discountValue(domain.getDiscountValue())
                .originalAmount(domain.getOriginalAmount())
                .discountAmount(domain.getDiscountAmount())
                .finalAmount(domain.getFinalAmount())
                .reason(domain.getReason())
                .status(domain.getStatus())
                .requestedBy(domain.getRequestedBy())
                .requestedAt(domain.getRequestedAt())
                .approvedBy(domain.getApprovedBy())
                .approvedAt(domain.getApprovedAt())
                .rejectedBy(domain.getRejectedBy())
                .rejectionReason(domain.getRejectionReason())
                .rejectedAt(domain.getRejectedAt())
                .invoiceId(domain.getInvoiceId())
                .build();
    }
}
