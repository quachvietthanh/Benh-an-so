package com.benhsoan.persistence.adapterRepository.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.persistence.entity.billing.PaymentEntity;
import com.benhsoan.persistence.jpaRepository.billing.JpaPaymentRepository;
import com.benhsoan.persistence.mapper.billing.PaymentPersistenceMapper;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryAdapterTest {

    @Mock
    private JpaPaymentRepository jpaRepository;

    @Spy
    private PaymentPersistenceMapper mapper = new PaymentPersistenceMapper();

    @InjectMocks
    private PaymentRepositoryAdapter adapter;

    @Test
    void findsPaymentForUpdate() {
        UUID paymentId = UUID.randomUUID();
        when(jpaRepository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(paymentEntity(paymentId)));

        var result = adapter.findByIdForUpdate(paymentId);

        assertEquals(paymentId, result.orElseThrow().getId());
        verify(jpaRepository).findByIdForUpdate(paymentId);
    }

    @Test
    void returnsZeroWhenNoRefundExistsInPeriod() {
        Instant from = Instant.parse("2026-08-18T00:00:00Z");
        Instant to = Instant.parse("2026-08-19T00:00:00Z");
        when(jpaRepository.sumRefundedAmountBetween(from, to)).thenReturn(null);

        BigDecimal result = adapter.sumRefundedAmountByRefundedAtBetween(from, to);

        assertEquals(BigDecimal.ZERO, result);
    }

    private PaymentEntity paymentEntity(UUID paymentId) {
        Instant paidAt = Instant.parse("2026-08-17T03:00:00Z");
        return PaymentEntity.builder()
                .id(paymentId)
                .visitId(UUID.randomUUID())
                .examFee(new BigDecimal("100000"))
                .medicineFee(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100000"))
                .amountPaid(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.CASH)
                .status(PaymentStatus.RECORDED)
                .collectedBy(UUID.randomUUID())
                .paidAt(paidAt)
                .createdAt(paidAt)
                .build();
    }
}
