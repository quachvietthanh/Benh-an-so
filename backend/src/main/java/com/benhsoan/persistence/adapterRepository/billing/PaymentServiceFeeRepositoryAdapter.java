package com.benhsoan.persistence.adapterRepository.billing;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.billing.PaymentServiceFee;
import com.benhsoan.persistence.jpaRepository.billing.JpaPaymentServiceFeeRepository;
import com.benhsoan.persistence.mapper.billing.PaymentServiceFeePersistenceMapper;
import com.benhsoan.port.outbound.repository.billing.PaymentServiceFeeRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentServiceFeeRepositoryAdapter implements PaymentServiceFeeRepository {

    private final JpaPaymentServiceFeeRepository jpaRepository;
    private final PaymentServiceFeePersistenceMapper mapper;

    @Override
    public List<PaymentServiceFee> saveAll(Collection<PaymentServiceFee> fees) {
        if (fees == null || fees.isEmpty()) {
            return List.of();
        }
        return jpaRepository.saveAll(fees.stream().map(mapper::toEntity).toList()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PaymentServiceFee> findAllByPaymentId(UUID paymentId) {
        if (paymentId == null) {
            return List.of();
        }
        return jpaRepository.findAllByPaymentIdOrderByCreatedAtAsc(paymentId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
