package com.benhsoan.persistence.mapper.billing;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.Payment;
import com.benhsoan.persistence.entity.billing.PaymentEntity;
import com.benhsoan.persistence.entity.billing.PaymentMethodItemEntity;

@Component
public class PaymentPersistenceMapper {

    private final PaymentMethodItemPersistenceMapper itemMapper;

    public PaymentPersistenceMapper() {
        this(new PaymentMethodItemPersistenceMapper());
    }

    public PaymentPersistenceMapper(PaymentMethodItemPersistenceMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    public Payment toDomain(PaymentEntity entity) {
        return toDomain(entity, null);
    }

    public Payment toDomain(PaymentEntity entity, java.util.List<PaymentMethodItemEntity> itemEntities) {
        if (entity == null) {
            return null;
        }

        java.util.List<com.benhsoan.domain.billing.PaymentMethodItem> items = null;
        if (itemEntities != null) {
            items = itemEntities.stream()
                    .map(itemMapper::toDomain)
                    .toList();
        }

        return Payment.restore(
                entity.getId(),
                entity.getVisitId(),
                entity.getExamFee(),
                entity.getMedicineFee(),
                entity.getServiceFee(),
                entity.getTotalAmount(),
                entity.getAmountPaid(),
                entity.getPaymentMethod(),
                entity.getStatus(),
                entity.getCollectedBy(),
                entity.getPaidAt(),
                entity.getRefundReason(),
                entity.getRefundedBy(),
                entity.getRefundedAt(),
                entity.getCreatedAt(),
<<<<<<< HEAD
                entity.getCashierShiftId(),
                items
=======
                entity.getCashierShiftId()
>>>>>>> 49e54faef023bb919dce508eec3bf599f4759064
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
                .serviceFee(domain.getServiceFee())
                .totalAmount(domain.getTotalAmount())
                .amountPaid(domain.getAmountPaid())
                .paymentMethod(domain.getPaymentMethod())
                .status(domain.getStatus())
                .collectedBy(domain.getCollectedBy())
                .paidAt(domain.getPaidAt())
                .refundReason(domain.getRefundReason())
                .refundedBy(domain.getRefundedBy())
                .refundedAt(domain.getRefundedAt())
                .createdAt(domain.getCreatedAt())
                .cashierShiftId(domain.getCashierShiftId())
                .build();
    }
}
