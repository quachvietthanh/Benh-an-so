package com.benhsoan.persistence.adapterRepository.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.persistence.entity.billing.PaymentEntity;
import com.benhsoan.persistence.entity.billing.PaymentMethodItemEntity;
import com.benhsoan.persistence.jpaRepository.billing.JpaPaymentMethodItemRepository;
import com.benhsoan.persistence.jpaRepository.billing.JpaPaymentRepository;
import com.benhsoan.persistence.mapper.billing.PaymentMethodItemPersistenceMapper;
import com.benhsoan.persistence.mapper.billing.PaymentPersistenceMapper;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final JpaPaymentRepository jpaRepository;

    private final JpaPaymentMethodItemRepository itemJpaRepository;

    private final PaymentPersistenceMapper mapper;

    private final PaymentMethodItemPersistenceMapper itemMapper;

    @Override
    @Transactional
    public Payment save(Payment payment) {
        PaymentEntity savedEntity = jpaRepository.saveAndFlush(mapper.toEntity(payment));

        List<PaymentMethodItemEntity> savedItemEntities;
        List<PaymentMethodItemEntity> existingItems = itemJpaRepository.findAllByPaymentId(savedEntity.getId());
        if (existingItems.isEmpty() && payment.getPaymentMethodItems() != null && !payment.getPaymentMethodItems().isEmpty()) {
            List<PaymentMethodItemEntity> itemEntities = payment.getPaymentMethodItems().stream()
                    .map(itemMapper::toEntity)
                    .toList();
            savedItemEntities = itemJpaRepository.saveAll(itemEntities);
        } else {
            savedItemEntities = existingItems;
        }

        return mapper.toDomain(savedEntity, savedItemEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<Payment> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByVisitId(UUID visitId) {
        return jpaRepository.findByVisitId(visitId).map(this::toDomain);
    }

    private Payment toDomain(PaymentEntity entity) {
        List<PaymentMethodItemEntity> itemEntities = itemJpaRepository.findAllByPaymentId(entity.getId());
        return mapper.toDomain(entity, itemEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumAmountPaidByStatusInAndPaidAtBetween(
            Collection<PaymentStatus> statuses,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        BigDecimal sum = jpaRepository.sumAmountPaidBetween(statuses, fromInclusive, toExclusive);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumRefundedAmountByRefundedAtBetween(
            Instant fromInclusive,
            Instant toExclusive
    ) {
        BigDecimal sum = jpaRepository.sumRefundedAmountBetween(fromInclusive, toExclusive);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    @Transactional
    public java.util.List<Payment> saveAll(java.util.List<Payment> payments) {
        var entities = payments.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Payment> findUnsettledByCashier(
            UUID cashierId,
            Collection<PaymentStatus> statuses
    ) {
        return jpaRepository.findUnsettledByCashier(cashierId, statuses)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public java.util.List<Payment> findUnsettledByCashierForUpdate(
            UUID cashierId,
            Collection<PaymentStatus> statuses
    ) {
        return jpaRepository.findUnsettledByCashierForUpdate(cashierId, statuses)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Payment> findByCashierShiftId(UUID cashierShiftId) {
        return jpaRepository.findByCashierShiftId(cashierShiftId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
