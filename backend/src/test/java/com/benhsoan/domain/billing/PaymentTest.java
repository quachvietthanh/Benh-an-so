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
    void recordIncludesClinicalServiceFeeInTotal() {
        Payment payment = Payment.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("95000"),
                new BigDecimal("345000"),
                PaymentMethod.CASH,
                UUID.randomUUID(),
                Instant.parse("2026-08-11T03:00:00Z"),
                VisitStatus.COMPLETED,
                true
        );

        assertEquals(new BigDecimal("95000"), payment.getServiceFee());
        assertEquals(new BigDecimal("345000"), payment.getTotalAmount());
    }

    @Test
    @DisplayName("record should create payment before the visit is completed")
    void recordShouldSucceedForWaitingVisit() {
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
                VisitStatus.WAITING,
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
                        VisitStatus.WAITING,
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
                        VisitStatus.WAITING,
                        false
                )
        );
    }

    @Test
    @DisplayName("record should reject payment for a cancelled visit")
    void recordShouldRejectCancelledVisit() {
        assertThrows(
                PaymentNotAllowedException.class,
                () -> Payment.record(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100000"),
                        BigDecimal.ZERO,
                        new BigDecimal("100000"),
                        PaymentMethod.CASH,
                        UUID.randomUUID(),
                        Instant.parse("2026-08-11T03:00:00Z"),
                        VisitStatus.CANCELLED,
                        true
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
        UUID refundedBy = UUID.randomUUID();
        Instant refundedAt = Instant.parse("2026-08-12T03:00:00Z");
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

        payment.refund(
                "  Patient cancelled after payment review  ",
                refundedBy,
                refundedAt
        );

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals("Patient cancelled after payment review", payment.getRefundReason());
        assertEquals(refundedBy, payment.getRefundedBy());
        assertEquals(refundedAt, payment.getRefundedAt());
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
                () -> payment.refund(" ", UUID.randomUUID(), Instant.now())
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
                () -> payment.refund(
                        "Cancel receipt before settlement",
                        UUID.randomUUID(),
                        Instant.now()
                )
        );
    }

    @Test
    @DisplayName("refund should require refund time")
    void refundShouldRequireRefundTime() {
        Payment payment = Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                new BigDecimal("100000"),
                new BigDecimal("100000"),
                PaymentMethod.CASH,
                PaymentStatus.SUCCESS,
                UUID.randomUUID(),
                Instant.parse("2026-08-11T03:00:00Z"),
                Instant.parse("2026-08-11T03:00:00Z")
        );

        assertThrows(
                ValidationException.class,
                () -> payment.refund("Patient cancelled", UUID.randomUUID(), null)
        );
    }
}
