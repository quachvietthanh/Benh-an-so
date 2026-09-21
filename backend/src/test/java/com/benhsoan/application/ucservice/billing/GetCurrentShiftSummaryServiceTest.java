package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.port.dto.result.CurrentShiftSummaryResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetCurrentShiftSummaryService Tests")
class GetCurrentShiftSummaryServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private UserRepository userRepository;
    @Mock private ClockPort clockPort;

    private GetCurrentShiftSummaryService service;

    private final UUID cashierId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-21T09:00:00Z");

    @BeforeEach
    void setUp() {
        service = new GetCurrentShiftSummaryService(
                paymentRepository,
                currentUserPort,
                userRepository,
                clockPort
        );

        when(currentUserPort.getCurrentUserId()).thenReturn(cashierId);
        when(clockPort.now()).thenReturn(now);
    }

    @Test
    @DisplayName("Khi không có khoản thu chưa chốt nào, trả về tổng tiền 0 và danh sách rỗng")
    void shouldReturnEmptySummaryWhenNoUnsettledPayments() {
        when(paymentRepository.findUnsettledByCashier(eq(cashierId), any()))
                .thenReturn(List.of());
        when(userRepository.findById(cashierId)).thenReturn(Optional.empty());

        CurrentShiftSummaryResult result = service.getCurrentSummary();

        assertEquals(cashierId, result.cashierId());
        assertEquals(0, result.totalTransactions());
        assertEquals(BigDecimal.ZERO, result.totalSystemAmount());
        assertEquals(BigDecimal.ZERO, result.systemCashAmount());
        assertEquals(BigDecimal.ZERO, result.systemTransferAmount());
        assertTrue(result.unsettledPaymentIds().isEmpty());
        assertNull(result.startTime());
        assertEquals(now, result.endTime());
    }

    @Test
    @DisplayName("Tổng hợp chính xác các khoản thu theo từng phương thức thanh toán")
    void shouldAggregatePaymentsByMethodAccurately() {
        User cashier = User.restore(
                cashierId,
                "receptionist1",
                "hash",
                "Nguyễn Thị Lễ Tân",
                "letan@benhsoan.com",
                "0900000000",
                UUID.randomUUID(),
                true,
                null,
                now
        );
        when(userRepository.findById(cashierId)).thenReturn(Optional.of(cashier));

        Instant p1Time = Instant.parse("2026-09-21T02:00:00Z");
        Instant p2Time = Instant.parse("2026-09-21T03:00:00Z");
        Instant p3Time = Instant.parse("2026-09-21T04:00:00Z");

        Payment p1 = Payment.restore(
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50000.00"), new BigDecimal("100000.00"), BigDecimal.ZERO,
                new BigDecimal("150000.00"), new BigDecimal("150000.00"),
                PaymentMethod.CASH, PaymentStatus.RECORDED, cashierId, p1Time, null, null, null, p1Time, null
        );
        Payment p2 = Payment.restore(
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50000.00"), new BigDecimal("250000.00"), BigDecimal.ZERO,
                new BigDecimal("300000.00"), new BigDecimal("300000.00"),
                PaymentMethod.BANK_TRANSFER, PaymentStatus.RECORDED, cashierId, p2Time, null, null, null, p2Time, null
        );
        Payment p3 = Payment.restore(
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("100000.00"), new BigDecimal("100000.00"),
                PaymentMethod.CASH, PaymentStatus.RECORDED, cashierId, p3Time, null, null, null, p3Time, null
        );

        when(paymentRepository.findUnsettledByCashier(eq(cashierId), any()))
                .thenReturn(List.of(p1, p2, p3));

        CurrentShiftSummaryResult result = service.getCurrentSummary();

        assertEquals(cashierId, result.cashierId());
        assertEquals("Nguyễn Thị Lễ Tân", result.cashierName());
        assertEquals(3, result.totalTransactions());
        assertEquals(new BigDecimal("550000.00"), result.totalSystemAmount());
        assertEquals(new BigDecimal("250000.00"), result.systemCashAmount());
        assertEquals(new BigDecimal("300000.00"), result.systemTransferAmount());
        assertEquals(BigDecimal.ZERO, result.systemCardAmount());
        assertEquals(p1Time, result.startTime());
        assertEquals(now, result.endTime());
        assertEquals(3, result.unsettledPaymentIds().size());
    }
}
