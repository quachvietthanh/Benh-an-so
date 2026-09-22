package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.PaymentAlreadySettledException;
import com.benhsoan.domain.shared.exception.ValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Payment Settlement and Shift Guard Tests")
class PaymentSettlementTest {

    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID COLLECTOR_ID = UUID.randomUUID();
    private static final UUID SHIFT_ID = UUID.randomUUID();
    private static final Instant PAID_AT = Instant.parse("2026-09-21T02:00:00Z");

    @Test
    @DisplayName("assignToShift gán ca chốt và đánh dấu isSettled thành true")
    void shouldAssignShiftAndMarkSettled() {
        Payment payment = createSamplePayment(null);
        assertFalse(payment.isSettled());

        payment.assignToShift(SHIFT_ID);

        assertTrue(payment.isSettled());
        assertEquals(SHIFT_ID, payment.getCashierShiftId());
    }

    @Test
    @DisplayName("assignToShift cùng shiftId là idempotent, khác shiftId ném ValidationException")
    void shouldRejectReassignToDifferentShift() {
        Payment payment = createSamplePayment(SHIFT_ID);
        assertTrue(payment.isSettled());

        // Cùng shiftId -> không lỗi
        payment.assignToShift(SHIFT_ID);

        // Khác shiftId -> ném lỗi
        UUID anotherShiftId = UUID.randomUUID();
        assertThrows(ValidationException.class, () -> payment.assignToShift(anotherShiftId));
    }

    @Test
    @DisplayName("TC-03: Khoản thu đã thuộc ca chốt bị chặn hoàn tiền và ném PaymentAlreadySettledException")
    void shouldRejectRefundWhenPaymentIsSettled() {
        Payment payment = createSamplePayment(SHIFT_ID);
        assertTrue(payment.isSettled());

        assertThrows(PaymentAlreadySettledException.class, () ->
                payment.refund("Hoàn tiền sau chốt ca", UUID.randomUUID(), Instant.now())
        );
    }

    @Test
    @DisplayName("Khoản thu chưa chốt ca (cashierShiftId == null) vẫn được hoàn tiền bình thường")
    void shouldAllowRefundWhenPaymentIsNotSettled() {
        Payment payment = createSamplePayment(null);
        assertFalse(payment.isSettled());

        UUID managerId = UUID.randomUUID();
        Instant refundTime = Instant.now();
        payment.refund("Lý do hoàn tiền", managerId, refundTime);

        assertTrue(payment.isRefunded());
        assertEquals("Lý do hoàn tiền", payment.getRefundReason());
        assertEquals(managerId, payment.getRefundedBy());
        assertEquals(refundTime, payment.getRefundedAt());
    }

    private Payment createSamplePayment(UUID shiftId) {
        return Payment.restore(
                UUID.randomUUID(),
                VISIT_ID,
                new BigDecimal("100000.00"),
                new BigDecimal("150000.00"),
                BigDecimal.ZERO,
                new BigDecimal("250000.00"),
                new BigDecimal("250000.00"),
                PaymentMethod.CASH,
                PaymentStatus.RECORDED,
                COLLECTOR_ID,
                PAID_AT,
                null,
                null,
                null,
                PAID_AT,
                shiftId
        );
    }
}
