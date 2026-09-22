package com.benhsoan.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.billing.enums.CashierShiftStatus;
import com.benhsoan.domain.billing.exception.CashierShiftAlreadyConfirmedException;
import com.benhsoan.domain.billing.exception.CashierShiftNoteRequiredException;
import com.benhsoan.domain.shared.exception.ValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CashierShift Domain Tests")
class CashierShiftTest {

    private static final UUID CASHIER_ID = UUID.randomUUID();
    private static final UUID MANAGER_ID = UUID.randomUUID();
    private static final Instant START_TIME = Instant.parse("2026-09-21T01:00:00Z");
    private static final Instant END_TIME = Instant.parse("2026-09-21T09:00:00Z");

    @Test
    @DisplayName("TC-01: Chốt ca khớp quỹ (difference = 0) tạo phiếu ở trạng thái CONFIRMED")
    void shouldCreateConfirmedShiftWhenCashMatches() {
        CashierShift shift = CashierShift.create(
                UUID.randomUUID(),
                "CS000001",
                CASHIER_ID,
                START_TIME,
                END_TIME,
                5,
                new BigDecimal("2000000.00"),
                new BigDecimal("1500000.00"),
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1500000.00"),
                null,
                END_TIME
        );

        assertEquals("CS000001", shift.getShiftCode());
        assertEquals(CASHIER_ID, shift.getCashierId());
        assertEquals(5, shift.getTotalTransactions());
        assertEquals(new BigDecimal("2000000.00"), shift.getTotalSystemAmount());
        assertEquals(new BigDecimal("1500000.00"), shift.getSystemCashAmount());
        assertEquals(new BigDecimal("1500000.00"), shift.getActualCashAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(shift.getDifferenceAmount()));
        assertEquals(CashierShiftStatus.CONFIRMED, shift.getStatus());
        assertTrue(shift.isConfirmed());
        assertNull(shift.getConfirmedBy());
    }

    @Test
    @DisplayName("TC-02: Chốt ca lệch quỹ âm (difference < 0) kèm ghi chú tạo phiếu ở trạng thái PENDING_CONFIRMATION")
    void shouldCreatePendingConfirmationShiftWhenCashDiscrepancyWithNotes() {
        CashierShift shift = CashierShift.create(
                UUID.randomUUID(),
                "CS000002",
                CASHIER_ID,
                START_TIME,
                END_TIME,
                3,
                new BigDecimal("1000000.00"),
                new BigDecimal("1000000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("950000.00"),
                "Thối nhầm tiền cho bệnh nhân",
                END_TIME
        );

        assertEquals(new BigDecimal("950000.00"), shift.getActualCashAmount());
        assertEquals(new BigDecimal("-50000.00"), shift.getDifferenceAmount());
        assertEquals(CashierShiftStatus.PENDING_CONFIRMATION, shift.getStatus());
        assertFalse(shift.isConfirmed());
        assertEquals("Thối nhầm tiền cho bệnh nhân", shift.getNotes());
    }

    @Test
    @DisplayName("TC-02: Chốt ca lệch quỹ mà không có ghi chú giải trình phải ném ngoại lệ CashierShiftNoteRequiredException")
    void shouldThrowWhenCashDiscrepancyWithoutNotes() {
        assertThrows(CashierShiftNoteRequiredException.class, () -> CashierShift.create(
                UUID.randomUUID(),
                "CS000003",
                CASHIER_ID,
                START_TIME,
                END_TIME,
                2,
                new BigDecimal("500000.00"),
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("480000.00"),
                "   ",
                END_TIME
        ));
    }

    @Test
    @DisplayName("Quản lý phòng khám xác nhận phiếu chốt ca thành công")
    void shouldConfirmShiftSuccessfully() {
        CashierShift shift = CashierShift.create(
                UUID.randomUUID(),
                "CS000004",
                CASHIER_ID,
                START_TIME,
                END_TIME,
                1,
                new BigDecimal("500000.00"),
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("450000.00"),
                "Lệch 50k",
                END_TIME
        );

        Instant confirmTime = END_TIME.plusSeconds(1800);
        shift.confirm(MANAGER_ID, confirmTime, "Đã duyệt và trừ quỹ");

        assertEquals(CashierShiftStatus.CONFIRMED, shift.getStatus());
        assertTrue(shift.isConfirmed());
        assertEquals(MANAGER_ID, shift.getConfirmedBy());
        assertEquals(confirmTime, shift.getConfirmedAt());
        assertEquals("Đã duyệt và trừ quỹ", shift.getConfirmationNotes());
    }

    @Test
    @DisplayName("Không được xác nhận lại phiếu chốt ca đã CONFIRMED")
    void shouldThrowWhenConfirmingAlreadyConfirmedShift() {
        CashierShift shift = CashierShift.create(
                UUID.randomUUID(),
                "CS000005",
                CASHIER_ID,
                START_TIME,
                END_TIME,
                1,
                new BigDecimal("500000.00"),
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("500000.00"),
                null,
                END_TIME
        );

        // Khớp quỹ -> ban đầu đã CONFIRMED
        assertTrue(shift.isConfirmed());

        assertThrows(CashierShiftAlreadyConfirmedException.class, () ->
                shift.confirm(MANAGER_ID, END_TIME.plusSeconds(300), "Duyệt lại")
        );
    }

    @Test
    @DisplayName("Validate thời gian bắt đầu sau thời gian kết thúc phải ném ValidationException")
    void shouldThrowWhenStartTimeAfterEndTime() {
        assertThrows(ValidationException.class, () -> CashierShift.create(
                UUID.randomUUID(),
                "CS000006",
                CASHIER_ID,
                END_TIME.plusSeconds(60),
                END_TIME,
                1,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                null,
                END_TIME
        ));
    }

    @Test
    @DisplayName("Validate số tiền mặt thực tế âm phải ném ValidationException")
    void shouldThrowWhenActualCashNegative() {
        assertThrows(ValidationException.class, () -> CashierShift.create(
                UUID.randomUUID(),
                "CS000007",
                CASHIER_ID,
                START_TIME,
                END_TIME,
                1,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("-50000.00"),
                "Notes",
                END_TIME
        ));
    }

    @Test
    @DisplayName("P3-02: Quản lý không được tự duyệt phiếu chốt ca lệch quỹ của chính mình")
    void shouldThrowWhenManagerAttemptsToConfirmOwnShift() {
        CashierShift shift = CashierShift.create(
                UUID.randomUUID(),
                "CS000008",
                CASHIER_ID,
                START_TIME,
                END_TIME,
                1,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("80000.00"),
                "Lệch 20k",
                END_TIME
        );

        assertThrows(com.benhsoan.domain.billing.exception.SelfConfirmationNotAllowedException.class,
                () -> shift.confirm(CASHIER_ID, END_TIME.plusSeconds(300), "Tự duyệt"));
    }

    @Test
    @DisplayName("P3-02: Quản lý khác được phép duyệt phiếu chốt ca lệch quỹ")
    void shouldAllowDifferentManagerToConfirmShift() {
        UUID differentManagerId = UUID.randomUUID();
        CashierShift shift = CashierShift.create(
                UUID.randomUUID(),
                "CS000009",
                CASHIER_ID,
                START_TIME,
                END_TIME,
                1,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("80000.00"),
                "Lệch 20k",
                END_TIME
        );

        shift.confirm(differentManagerId, END_TIME.plusSeconds(300), "Duyệt bởi quản lý độc lập");
        assertTrue(shift.isConfirmed());
        assertEquals(differentManagerId, shift.getConfirmedBy());
    }

    @Test
    @DisplayName("P1: Tiền mặt hệ thống âm do hoàn tiền vẫn tạo phiếu hợp lệ và tính chênh lệch đúng")
    void shouldAllowNegativeSystemAmountsWhenRefundsExceedPayments() {
        CashierShift shift = CashierShift.create(
                UUID.randomUUID(),
                "CS000010",
                CASHIER_ID,
                START_TIME,
                END_TIME,
                2,
                new BigDecimal("-500000.00"),
                new BigDecimal("-500000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Hoàn tiền vượt số thu trong ca",
                END_TIME
        );

        assertEquals(new BigDecimal("-500000.00"), shift.getSystemCashAmount());
        assertEquals(BigDecimal.ZERO, shift.getActualCashAmount());
        assertEquals(new BigDecimal("500000.00"), shift.getDifferenceAmount());
        assertEquals(CashierShiftStatus.PENDING_CONFIRMATION, shift.getStatus());
    }
}
