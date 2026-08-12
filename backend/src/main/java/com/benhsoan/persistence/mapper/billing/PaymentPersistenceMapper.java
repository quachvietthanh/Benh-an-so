package com.benhsoan.persistence.mapper.billing;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.Payment;
import com.benhsoan.persistence.entity.billing.PaymentEntity;

@Component
public class PaymentPersistenceMapper {

    public Payment toDomain(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }

        return Payment.restore(
                entity.getId(),
                entity.getVisitId(),
                entity.getExamFee(),
                entity.getMedicineFee(),
                entity.getTotalAmount(),
                entity.getAmountPaid(),
                entity.getPaymentMethod(),
                entity.getStatus(),
                entity.getCollectedBy(),
                entity.getPaidAt(),
                entity.getCreatedAt()
        );
    }

    public PaymentEntity toEntity(Payment domain) {
        if (domain == null) {
            return null;
        }

        return PaymentEntity.builder()
                .id(domain.getId())
                .visitId(domain.getVisitId())
                .examFee(domain.getExamFee())
                .medicineFee(domain.getMedicineFee())
                .totalAmount(domain.getTotalAmount())
                .amountPaid(domain.getAmountPaid())
                .paymentMethod(domain.getPaymentMethod())
                .status(domain.getStatus())
                .collectedBy(domain.getCollectedBy())
                .paidAt(domain.getPaidAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
