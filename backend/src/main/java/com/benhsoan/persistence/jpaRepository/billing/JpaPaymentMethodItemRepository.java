package com.benhsoan.persistence.jpaRepository.billing;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.benhsoan.persistence.entity.billing.PaymentMethodItemEntity;

@Repository
public interface JpaPaymentMethodItemRepository extends JpaRepository<PaymentMethodItemEntity, UUID> {

    List<PaymentMethodItemEntity> findAllByPaymentId(UUID paymentId);

    void deleteAllByPaymentId(UUID paymentId);
}
