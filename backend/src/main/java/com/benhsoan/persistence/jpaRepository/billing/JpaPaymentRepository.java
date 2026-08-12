package com.benhsoan.persistence.jpaRepository.billing;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.billing.PaymentEntity;

public interface JpaPaymentRepository
        extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByVisitId(UUID visitId);
}
