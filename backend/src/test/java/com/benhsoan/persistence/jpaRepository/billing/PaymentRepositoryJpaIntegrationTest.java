package com.benhsoan.persistence.jpaRepository.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.persistence.entity.billing.PaymentEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PaymentRepositoryJpaIntegrationTest {

    private static final Instant FROM = Instant.parse("2026-08-18T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-08-19T00:00:00Z");

    @Autowired
    private JpaPaymentRepository repository;

    @Test
    void sumsOnlyRefundsInsideHalfOpenPeriod() {
        savePayment(PaymentStatus.REFUNDED, new BigDecimal("100000"), FROM);
        savePayment(PaymentStatus.REFUNDED, new BigDecimal("200000"), FROM.minusSeconds(1));
        savePayment(PaymentStatus.REFUNDED, new BigDecimal("300000"), TO);
        savePayment(PaymentStatus.SUCCESS, new BigDecimal("400000"), FROM.plusSeconds(60));

        BigDecimal result = repository.sumRefundedAmountBetween(FROM, TO);

        assertEquals(0, new BigDecimal("100000").compareTo(result));
    }

    @Test
    void keepsRefundedPaymentInPaidPeriodAndSubtractsItInRefundPeriod() {
        BigDecimal amount = new BigDecimal("250000");
        Instant paidPeriodFrom = Instant.parse("2026-08-17T00:00:00Z");
        Instant paidPeriodTo = Instant.parse("2026-08-18T00:00:00Z");
        savePayment(PaymentStatus.REFUNDED, amount, FROM.plusSeconds(60));

        BigDecimal originalCollections = repository.sumAmountPaidBetween(
                List.of(PaymentStatus.RECORDED, PaymentStatus.SUCCESS, PaymentStatus.REFUNDED),
                paidPeriodFrom,
                paidPeriodTo
        );
        BigDecimal refundsInPaidPeriod = repository.sumRefundedAmountBetween(
                paidPeriodFrom,
                paidPeriodTo
        );
        BigDecimal refundsInRefundPeriod = repository.sumRefundedAmountBetween(FROM, TO);

        assertEquals(0, amount.compareTo(originalCollections));
        assertNull(refundsInPaidPeriod);
        assertEquals(0, amount.compareTo(refundsInRefundPeriod));
    }

    @Test
    void includesActiveCollectionsAndExcludesCancelledPayments() {
        savePayment(PaymentStatus.SUCCESS, new BigDecimal("100000"), null);
        savePayment(PaymentStatus.REFUNDED, new BigDecimal("200000"), FROM.plusSeconds(60));
        savePayment(PaymentStatus.CANCELLED, new BigDecimal("400000"), null);

        BigDecimal collections = repository.sumAmountPaidBetween(
                List.of(PaymentStatus.RECORDED, PaymentStatus.SUCCESS, PaymentStatus.REFUNDED),
                Instant.parse("2026-08-17T00:00:00Z"),
                Instant.parse("2026-08-18T00:00:00Z")
        );

        assertEquals(0, new BigDecimal("300000").compareTo(collections));
    }

    @Test
    void netsCollectionAndRefundWhenBothOccurInSamePeriod() {
        BigDecimal amount = new BigDecimal("250000");
        Instant periodFrom = Instant.parse("2026-08-17T00:00:00Z");
        Instant periodTo = Instant.parse("2026-08-18T00:00:00Z");
        savePayment(PaymentStatus.REFUNDED, amount, periodFrom.plusSeconds(4 * 3600));

        BigDecimal collections = repository.sumAmountPaidBetween(
                List.of(PaymentStatus.RECORDED, PaymentStatus.SUCCESS, PaymentStatus.REFUNDED),
                periodFrom,
                periodTo
        );
        BigDecimal refunds = repository.sumRefundedAmountBetween(periodFrom, periodTo);

        assertEquals(0, BigDecimal.ZERO.compareTo(collections.subtract(refunds)));
    }

    @Test
    void findsPaymentWithPessimisticWriteQuery() {
        PaymentEntity payment = savePayment(
                PaymentStatus.RECORDED,
                new BigDecimal("100000"),
                null
        );

        PaymentEntity locked = repository.findByIdForUpdate(payment.getId()).orElseThrow();

        assertEquals(payment.getId(), locked.getId());
    }

    private PaymentEntity savePayment(
            PaymentStatus status,
            BigDecimal amount,
            Instant refundedAt
    ) {
        Instant paidAt = Instant.parse("2026-08-17T03:00:00Z");
        return repository.saveAndFlush(PaymentEntity.builder()
                .id(UUID.randomUUID())
                .visitId(UUID.randomUUID())
                .examFee(amount)
                .medicineFee(BigDecimal.ZERO)
                .serviceFee(BigDecimal.ZERO)
                .totalAmount(amount)
                .amountPaid(amount)
                .paymentMethod(PaymentMethod.CASH)
                .status(status)
                .collectedBy(UUID.randomUUID())
                .paidAt(paidAt)
                .refundReason(refundedAt == null ? null : "Refund test")
                .refundedBy(refundedAt == null ? null : UUID.randomUUID())
                .refundedAt(refundedAt)
                .createdAt(paidAt)
                .build());
    }
}
