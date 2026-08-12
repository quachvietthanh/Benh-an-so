package com.benhsoan.domain.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.PaymentAmountMismatchException;
import com.benhsoan.domain.billing.exception.PaymentNotAllowedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.enums.VisitStatus;

@DisplayName("Payment Domain Tests")
class PaymentTest {

    @Test
    @DisplayName("record should create payment when visit is completed and dispensing is done")
    void recordShouldSucceedForEligibleVisit() {
        Instant paidAt = Instant.parse("2026-08-11T03:00:00Z");

        Payment payment = Payment.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("250000"),
                PaymentMethod.CASH,
                UUID.randomUUID(),
                paidAt,
                VisitStatus.COMPLETED,
                true
        );

        assertEquals(new BigDecimal("250000"), payment.getTotalAmount());
        assertEquals(PaymentStatus.RECORDED, payment.getStatus());
        assertEquals(paidAt, payment.getPaidAt());
    }

    @Test
    @DisplayName("record should reject payment amount different from amount due")
    void recordShouldRejectDifferentPaymentAmount() {
        assertThrows(
                PaymentAmountMismatchException.class,
                () -> Payment.record(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100000"),
                        new BigDecimal("150000"),
                        new BigDecimal("200000"),
                        PaymentMethod.CASH,
                        UUID.randomUUID(),
                        Instant.parse("2026-08-11T03:00:00Z"),
                        VisitStatus.COMPLETED,
                        true
                )
        );
    }

    @Test
    @DisplayName("record should reject payment before dispensing is completed")
    void recordShouldRejectIncompleteDispensing() {
        assertThrows(
                PaymentNotAllowedException.class,
                () -> Payment.record(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100000"),
                        new BigDecimal("150000"),
                        new BigDecimal("250000"),
                        PaymentMethod.CARD,
                        UUID.randomUUID(),
                        Instant.parse("2026-08-11T03:00:00Z"),
                        VisitStatus.COMPLETED,
                        false
                )
        );
    }

    @Test
    @DisplayName("record should reject zero total payment")
    void recordShouldRejectZeroTotalPayment() {
        assertThrows(
                ValidationException.class,
                () -> Payment.record(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        PaymentMethod.CASH,
                        UUID.randomUUID(),
                        Instant.parse("2026-08-11T03:00:00Z"),
                        VisitStatus.COMPLETED,
                        true
                )
        );
    }

    @Test
    @DisplayName("refund should mark a recorded payment as refunded")
    void refundShouldMarkPaymentAsRefunded() {
        Payment payment = Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.CASH,
                PaymentStatus.RECORDED,
                UUID.randomUUID(),
                Instant.parse("2026-08-11T03:00:00Z"),
                Instant.parse("2026-08-11T03:00:00Z")
        );

        payment.refund("Patient cancelled after payment review", UUID.randomUUID());

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    @DisplayName("refund should require a reason")
    void refundShouldRequireReason() {
        Payment payment = Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.CASH,
                PaymentStatus.SUCCESS,
                UUID.randomUUID(),
                Instant.parse("2026-08-11T03:00:00Z"),
                Instant.parse("2026-08-11T03:00:00Z")
        );

        assertThrows(
                ValidationException.class,
                () -> payment.refund(" ", UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("refund should reject non-refundable status")
    void refundShouldRejectNonRefundableStatus() {
        Payment payment = Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.CASH,
                PaymentStatus.CANCELLED,
                UUID.randomUUID(),
                Instant.parse("2026-08-11T03:00:00Z"),
                Instant.parse("2026-08-11T03:00:00Z")
        );

        assertThrows(
                PaymentNotAllowedException.class,
                () -> payment.refund("Cancel receipt before settlement", UUID.randomUUID())
        );
    }
}
