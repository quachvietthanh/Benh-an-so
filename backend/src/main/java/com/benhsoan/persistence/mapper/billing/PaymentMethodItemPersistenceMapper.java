package com.benhsoan.persistence.mapper.billing;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.PaymentMethodItem;
import com.benhsoan.persistence.entity.billing.PaymentMethodItemEntity;

@Component
public class PaymentMethodItemPersistenceMapper {

    public PaymentMethodItem toDomain(PaymentMethodItemEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentMethodItem.restore(
                entity.getId(),
                entity.getPaymentId(),
                entity.getPaymentMethod(),
                entity.getAmount(),
                entity.getReferenceNumber(),
                entity.getCreatedAt()
        );
    }

    public PaymentMethodItemEntity toEntity(PaymentMethodItem domain) {
        if (domain == null) {
            return null;
        }

        return PaymentMethodItemEntity.builder()
                .id(domain.getId())
                .paymentId(domain.getPaymentId())
                .paymentMethod(domain.getPaymentMethod())
                .amount(domain.getAmount())
                .referenceNumber(domain.getReferenceNumber())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
