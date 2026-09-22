package com.benhsoan.domain.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    @Test
    @DisplayName("record with multiple payment methods should succeed and set MULTIPLE method (TC-01)")
    void recordWithMultiplePaymentMethodsSucceeds() {
        UUID paymentId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-11T03:00:00Z");

        List<PaymentMethodItem> items = List.of(
                PaymentMethodItem.create(UUID.randomUUID(), paymentId, PaymentMethod.CASH, new BigDecimal("100000"), null, paidAt),
                PaymentMethodItem.create(UUID.randomUUID(), paymentId, PaymentMethod.BANK_TRANSFER, new BigDecimal("150000"), "TXN123456", paidAt)
        );

        Payment payment = Payment.record(
                paymentId,
                UUID.randomUUID(),
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                BigDecimal.ZERO,
                new BigDecimal("250000"),
                items,
                UUID.randomUUID(),
                paidAt,
                VisitStatus.WAITING,
                true
        );

        assertEquals(PaymentMethod.MULTIPLE, payment.getPaymentMethod());
        assertEquals(2, payment.getPaymentMethodItems().size());
        assertEquals(new BigDecimal("250000"), payment.getAmountPaid());
        assertEquals(new BigDecimal("250000"), payment.getTotalAmount());
    }

    @Test
    @DisplayName("record with multiple payment methods should reject underpayment and report deficit (TC-02)")
    void recordWithMultipleMethodsRejectsUnderpayment() {
        UUID paymentId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-11T03:00:00Z");

        List<PaymentMethodItem> items = List.of(
                PaymentMethodItem.create(UUID.randomUUID(), paymentId, PaymentMethod.CASH, new BigDecimal("100000"), null, paidAt),
                PaymentMethodItem.create(UUID.randomUUID(), paymentId, PaymentMethod.BANK_TRANSFER, new BigDecimal("100000"), "TXN123456", paidAt)
        );

        PaymentAmountMismatchException ex = assertThrows(
                PaymentAmountMismatchException.class,
                () -> Payment.record(
                        paymentId,
                        UUID.randomUUID(),
                        new BigDecimal("100000"),
                        new BigDecimal("150000"),
                        BigDecimal.ZERO,
                        new BigDecimal("250000"),
                        items,
                        UUID.randomUUID(),
                        paidAt,
                        VisitStatus.WAITING,
                        true
                )
        );

        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("50000"));
    }

    @Test
    @DisplayName("PaymentMethodItem should require reference number for bank transfer (TC-03)")
    void bankTransferRequiresReferenceNumber() {
        UUID paymentId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-11T03:00:00Z");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> PaymentMethodItem.create(
                        UUID.randomUUID(),
                        paymentId,
                        PaymentMethod.BANK_TRANSFER,
                        new BigDecimal("150000"),
                        "   ",
                        paidAt
                )
        );

        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("reference number"));
    }

    @Test
    @DisplayName("record with single method items should resolve to that specific method")
    void recordWithSingleMethodItemsResolvesToSingleMethod() {
        UUID paymentId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-11T03:00:00Z");

        List<PaymentMethodItem> items = List.of(
                PaymentMethodItem.create(UUID.randomUUID(), paymentId, PaymentMethod.CASH, new BigDecimal("250000"), null, paidAt)
        );

        Payment payment = Payment.record(
                paymentId,
                UUID.randomUUID(),
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                BigDecimal.ZERO,
                new BigDecimal("250000"),
                items,
                UUID.randomUUID(),
                paidAt,
                VisitStatus.WAITING,
                true
        );

        assertEquals(PaymentMethod.CASH, payment.getPaymentMethod());
        assertEquals(1, payment.getPaymentMethodItems().size());
    }

    @Test
    @DisplayName("PaymentMethodItem should reject reference number exceeding 100 characters (Finding P2)")
    void rejectsReferenceNumberExceeding100Chars() {
        UUID paymentId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-11T03:00:00Z");
        String longRef = "R".repeat(101);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> PaymentMethodItem.create(
                        UUID.randomUUID(),
                        paymentId,
                        PaymentMethod.BANK_TRANSFER,
                        new BigDecimal("150000"),
                        longRef,
                        paidAt
                )
        );

        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    @DisplayName("PaymentMethodItem should accept reference number with exactly 100 characters (Finding P2)")
    void acceptsReferenceNumberWith100Chars() {
        UUID paymentId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-11T03:00:00Z");
        String ref100 = "R".repeat(100);

        PaymentMethodItem item = PaymentMethodItem.create(
                UUID.randomUUID(),
                paymentId,
                PaymentMethod.BANK_TRANSFER,
                new BigDecimal("150000"),
                ref100,
                paidAt
        );

        org.junit.jupiter.api.Assertions.assertEquals(100, item.getReferenceNumber().length());
    }

    @Test
    @DisplayName("PaymentMethodItem should trim reference number and accept if trimmed length <= 100 (Finding P2)")
    void trimsReferenceNumberBeforeLengthCheck() {
        UUID paymentId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-11T03:00:00Z");
        String paddedRef = "   " + "R".repeat(100) + "   ";

        PaymentMethodItem item = PaymentMethodItem.create(
                UUID.randomUUID(),
                paymentId,
                PaymentMethod.BANK_TRANSFER,
                new BigDecimal("150000"),
                paddedRef,
                paidAt
        );

        org.junit.jupiter.api.Assertions.assertEquals(100, item.getReferenceNumber().length());
    }

    @Test
    @DisplayName("computesMethodAmountsCorrectlyViaDomainHelpers: calculates cash, bank transfer, and custom amounts correctly (Finding P2 / QTN-38)")
    void computesMethodAmountsCorrectlyViaDomainHelpers() {
        UUID paymentId = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-08-11T03:00:00Z");

        Payment payment = Payment.record(
                paymentId,
                UUID.randomUUID(),
                new BigDecimal("200000"),
                new BigDecimal("300000"),
                BigDecimal.ZERO,
                new BigDecimal("500000"),
                List.of(
                        PaymentMethodItem.create(UUID.randomUUID(), paymentId, PaymentMethod.CASH, new BigDecimal("200000"), null, paidAt),
                        PaymentMethodItem.create(UUID.randomUUID(), paymentId, PaymentMethod.BANK_TRANSFER, new BigDecimal("300000"), "TXN-001", paidAt)
                ),
                UUID.randomUUID(),
                paidAt,
                VisitStatus.COMPLETED,
                true
        );

        assertEquals(0, new BigDecimal("200000").compareTo(payment.getCashAmount()));
        assertEquals(0, new BigDecimal("300000").compareTo(payment.getBankTransferAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(payment.getAmountByMethod(PaymentMethod.CARD)));
        assertEquals(0, new BigDecimal("200000").compareTo(payment.getAmountByMethod(PaymentMethod.CASH)));
        assertEquals(PaymentMethod.MULTIPLE, payment.getPaymentMethod());
    }

    @Test
    @DisplayName("computesMethodAmountsCorrectlyForLegacySingleMethodPayment: fallback to paymentMethod and amountPaid when items are empty")
    void computesMethodAmountsCorrectlyForLegacySingleMethodPayment() {
        Payment legacyPayment = Payment.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                BigDecimal.ZERO,
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.CASH,
                PaymentStatus.RECORDED,
                UUID.randomUUID(),
                Instant.parse("2026-08-11T03:00:00Z"),
                null,
                null,
                null,
                Instant.parse("2026-08-11T03:00:00Z"),
                List.of()
        );

        assertEquals(0, new BigDecimal("250000").compareTo(legacyPayment.getCashAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(legacyPayment.getBankTransferAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(legacyPayment.getAmountByMethod(PaymentMethod.CARD)));
    }
}
