package com.benhsoan.persistence.mapper.billing;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.PaymentServiceFee;
import com.benhsoan.persistence.entity.billing.PaymentServiceFeeEntity;

@Component
public class PaymentServiceFeePersistenceMapper {

    public PaymentServiceFee toDomain(PaymentServiceFeeEntity entity) {
        if (entity == null) {
            return null;
        }
        return PaymentServiceFee.restore(
                entity.getId(),
                entity.getPaymentId(),
                entity.getClinicalOrderItemId(),
                entity.getServiceName(),
                entity.getAmount(),
                entity.getCreatedAt()
        );
    }

    public PaymentServiceFeeEntity toEntity(PaymentServiceFee domain) {
        if (domain == null) {
            return null;
        }
        return PaymentServiceFeeEntity.builder()
                .id(domain.getId())
                .paymentId(domain.getPaymentId())
                .clinicalOrderItemId(domain.getClinicalOrderItemId())
                .serviceName(domain.getServiceName())
                .amount(domain.getAmount())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
