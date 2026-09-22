package com.benhsoan.domain.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.persistence.adapterRepository.billing.PaymentRepositoryAdapter;
import com.benhsoan.persistence.entity.billing.PaymentEntity;
import com.benhsoan.persistence.entity.billing.PaymentMethodItemEntity;
import com.benhsoan.persistence.jpaRepository.billing.JpaPaymentMethodItemRepository;
import com.benhsoan.persistence.jpaRepository.billing.JpaPaymentRepository;
import com.benhsoan.persistence.mapper.billing.PaymentMethodItemPersistenceMapper;
import com.benhsoan.persistence.mapper.billing.PaymentPersistenceMapper;

@DisplayName("Payment Immutability Tests (QTN-09)")
class PaymentImmutabilityTest {

    @Test
    @DisplayName("Payment entity should have no public setter methods")
    void paymentHasNoPublicSetters() {
        Method[] methods = Payment.class.getMethods();
        List<Method> setters = Arrays.stream(methods)
                .filter(m -> m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers()))
                .toList();

        assertTrue(setters.isEmpty(), "Payment domain entity must not expose any public setter methods (QTN-09)");
    }

    @Test
    @DisplayName("PaymentMethodItem entity should have no public setter methods")
    void paymentMethodItemHasNoPublicSetters() {
        Method[] methods = PaymentMethodItem.class.getMethods();
        List<Method> setters = Arrays.stream(methods)
                .filter(m -> m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers()))
                .toList();

        assertTrue(setters.isEmpty(), "PaymentMethodItem domain entity must not expose any public setter methods (QTN-09)");
    }

    @Test
    @DisplayName("Refunding payment should only update status and refund audit fields, keeping items and amounts immutable")
    void refundPreservesPaymentAmountsAndMethodItems() {
        UUID paymentId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-11T03:00:00Z");
        Instant refundedAt = Instant.parse("2026-08-11T05:00:00Z");

        PaymentMethodItem cashItem = PaymentMethodItem.create(
                UUID.randomUUID(), paymentId, PaymentMethod.CASH, new BigDecimal("100000"), null, paidAt
        );
        PaymentMethodItem transferItem = PaymentMethodItem.create(
                UUID.randomUUID(), paymentId, PaymentMethod.BANK_TRANSFER, new BigDecimal("150000"), "TXN-001", paidAt
        );

        Payment payment = Payment.restore(
                paymentId,
                visitId,
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                BigDecimal.ZERO,
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.MULTIPLE,
                PaymentStatus.RECORDED,
                actorId,
                paidAt,
                null,
                null,
                null,
                paidAt,
                List.of(cashItem, transferItem)
        );

        payment.refund("Patient requested cancellation", actorId, refundedAt);
        Payment refunded = payment;

        assertEquals(PaymentStatus.REFUNDED, refunded.getStatus());
        assertEquals("Patient requested cancellation", refunded.getRefundReason());
        assertEquals(actorId, refunded.getRefundedBy());
        assertEquals(refundedAt, refunded.getRefundedAt());

        // Immutability checks: financial figures and method items remain identical
        assertEquals(new BigDecimal("100000"), refunded.getExamFee());
        assertEquals(new BigDecimal("150000"), refunded.getMedicineFee());
        assertEquals(new BigDecimal("250000"), refunded.getTotalAmount());
        assertEquals(new BigDecimal("250000"), refunded.getAmountPaid());
        assertEquals(PaymentMethod.MULTIPLE, refunded.getPaymentMethod());
        assertEquals(2, refunded.getPaymentMethodItems().size());
        assertEquals("TXN-001", refunded.getPaymentMethodItems().get(1).getReferenceNumber());
    }

    @Test
    @DisplayName("PaymentRepositoryAdapter.save should not delete items on update (QTN-09)")
    void repositoryAdapterDoesNotDeleteItemsOnUpdate() {
        JpaPaymentRepository jpaRepository = mock(JpaPaymentRepository.class);
        JpaPaymentMethodItemRepository itemJpaRepository = mock(JpaPaymentMethodItemRepository.class);
        PaymentMethodItemPersistenceMapper itemMapper = new PaymentMethodItemPersistenceMapper();
        PaymentPersistenceMapper mapper = new PaymentPersistenceMapper(itemMapper);

        PaymentRepositoryAdapter adapter = new PaymentRepositoryAdapter(
                jpaRepository, itemJpaRepository, mapper, itemMapper
        );

        UUID paymentId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Instant now = Instant.now();

        PaymentEntity existingEntity = PaymentEntity.builder()
                .id(paymentId)
                .visitId(visitId)
                .examFee(new BigDecimal("100000"))
                .medicineFee(new BigDecimal("150000"))
                .serviceFee(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("250000"))
                .amountPaid(new BigDecimal("250000"))
                .paymentMethod(PaymentMethod.MULTIPLE)
                .status(PaymentStatus.RECORDED)
                .collectedBy(UUID.randomUUID())
                .paidAt(now)
                .createdAt(now)
                .build();

        when(jpaRepository.saveAndFlush(any())).thenReturn(existingEntity);
        // Existing items already in database
        when(itemJpaRepository.findAllByPaymentId(paymentId)).thenReturn(List.of(
                new PaymentMethodItemEntity(UUID.randomUUID(), paymentId, PaymentMethod.CASH, new BigDecimal("100000"), null, now),
                new PaymentMethodItemEntity(UUID.randomUUID(), paymentId, PaymentMethod.BANK_TRANSFER, new BigDecimal("150000"), "TXN-01", now)
        ));

        Payment payment = Payment.restore(
                paymentId, visitId, new BigDecimal("100000"), new BigDecimal("150000"),
                BigDecimal.ZERO, new BigDecimal("250000"), new BigDecimal("250000"),
                PaymentMethod.MULTIPLE, PaymentStatus.REFUNDED, UUID.randomUUID(),
                now, "Reason", UUID.randomUUID(), now, now,
                List.of(
                        PaymentMethodItem.create(UUID.randomUUID(), paymentId, PaymentMethod.CASH, new BigDecimal("100000"), null, now),
                        PaymentMethodItem.create(UUID.randomUUID(), paymentId, PaymentMethod.BANK_TRANSFER, new BigDecimal("150000"), "TXN-01", now)
                )
        );

        Payment saved = adapter.save(payment);

        assertNotNull(saved);
        // CRITICAL CHECK: deleteAllByPaymentId must NEVER be called
        verify(itemJpaRepository, never()).deleteAllByPaymentId(any());
    }
}
