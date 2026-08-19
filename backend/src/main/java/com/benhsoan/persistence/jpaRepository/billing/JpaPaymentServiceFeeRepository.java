package com.benhsoan.persistence.jpaRepository.billing;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.billing.PaymentServiceFeeEntity;

public interface JpaPaymentServiceFeeRepository extends JpaRepository<PaymentServiceFeeEntity, UUID> {

    List<PaymentServiceFeeEntity> findAllByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
}
