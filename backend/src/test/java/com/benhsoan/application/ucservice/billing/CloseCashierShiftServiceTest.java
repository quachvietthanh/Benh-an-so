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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.billing.CashierShift;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.CashierShiftStatus;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.CashierShiftNoteRequiredException;
import com.benhsoan.domain.billing.exception.NoUnsettledPaymentsException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.billing.CloseCashierShiftCommand;
import com.benhsoan.port.dto.result.CashierShiftResult;
import com.benhsoan.port.outbound.generator.CashierShiftCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.billing.CashierShiftRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloseCashierShiftService Tests")
class CloseCashierShiftServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private CashierShiftRepository cashierShiftRepository;
    @Mock private CashierShiftCodeGenerator shiftCodeGenerator;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private CashierShiftAuthorizationAuditService authorizationAuditService;

    private CloseCashierShiftService service;
    private CashierShiftResultMapper resultMapper;

    private final UUID cashierId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-21T09:00:00Z");

    @BeforeEach
    void setUp() {
        resultMapper = new CashierShiftResultMapper(userRepository);
        service = new CloseCashierShiftService(
                paymentRepository,
                cashierShiftRepository,
                shiftCodeGenerator,
                currentUserPort,
                clockPort,
                auditLogRepository,
                resultMapper,
                authorizationAuditService,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );

        lenient().when(currentUserPort.getCurrentUserId()).thenReturn(cashierId);
        lenient().when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        lenient().when(clockPort.now()).thenReturn(now);
    }

    @Test
    @DisplayName("Precondition: Khi ca làm việc chưa có khoản thu nào, ném NoUnsettledPaymentsException")
    void shouldThrowWhenNoUnsettledPayments() {
        when(paymentRepository.findUnsettledByCashierForUpdate(eq(cashierId), any()))
                .thenReturn(List.of());

        CloseCashierShiftCommand command = new CloseCashierShiftCommand(
                BigDecimal.ZERO,
                null
        );

        assertThrows(NoUnsettledPaymentsException.class, () -> service.close(command));
    }

    @Test
    @DisplayName("TC-01: Chốt ca thành công khi tiền mặt thực đếm khớp số liệu hệ thống")
    void shouldCloseShiftSuccessfullyWhenCashMatches() {
        Instant p1Time = Instant.parse("2026-09-21T02:00:00Z");
        Payment p1 = createPayment(new BigDecimal("200000.00"), PaymentMethod.CASH, p1Time);
        Payment p2 = createPayment(new BigDecimal("300000.00"), PaymentMethod.BANK_TRANSFER, p1Time);

        when(paymentRepository.findUnsettledByCashierForUpdate(eq(cashierId), any()))
                .thenReturn(List.of(p1, p2));
        when(shiftCodeGenerator.generate()).thenReturn("CS000001");
        when(cashierShiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CloseCashierShiftCommand command = new CloseCashierShiftCommand(
                new BigDecimal("200000.00"),
                null
        );

        CashierShiftResult result = service.close(command);

        assertEquals("CS000001", result.shiftCode());
        assertEquals(cashierId, result.cashierId());
        assertEquals(2, result.totalTransactions());
        assertEquals(new BigDecimal("500000.00"), result.totalSystemAmount());
        assertEquals(new BigDecimal("200000.00"), result.systemCashAmount());
        assertEquals(new BigDecimal("300000.00"), result.systemTransferAmount());
        assertEquals(new BigDecimal("200000.00"), result.actualCashAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.differenceAmount()));
        assertEquals(CashierShiftStatus.CONFIRMED, result.status());

        // Kiểm tra toàn bộ payment được gán shiftId
        assertTrue(p1.isSettled());
        assertTrue(p2.isSettled());
        assertEquals(result.id(), p1.getCashierShiftId());
        assertEquals(result.id(), p2.getCashierShiftId());

        verify(paymentRepository).saveAll(List.of(p1, p2));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-02: Lệch quỹ âm và có ghi chú -> tạo phiếu PENDING_CONFIRMATION")
    void shouldCreatePendingConfirmationShiftWhenDiscrepancyWithNotes() {
        Instant p1Time = Instant.parse("2026-09-21T02:00:00Z");
        Payment p1 = createPayment(new BigDecimal("500000.00"), PaymentMethod.CASH, p1Time);

        when(paymentRepository.findUnsettledByCashierForUpdate(eq(cashierId), any()))
                .thenReturn(List.of(p1));
        when(shiftCodeGenerator.generate()).thenReturn("CS000002");
        when(cashierShiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CloseCashierShiftCommand command = new CloseCashierShiftCommand(
                new BigDecimal("480000.00"),
                "Thối nhầm 20k"
        );

        CashierShiftResult result = service.close(command);

        assertEquals(new BigDecimal("480000.00"), result.actualCashAmount());
        assertEquals(new BigDecimal("-20000.00"), result.differenceAmount());
        assertEquals(CashierShiftStatus.PENDING_CONFIRMATION, result.status());
        assertEquals("Thối nhầm 20k", result.notes());
    }

    @Test
    @DisplayName("TC-02: Lệch quỹ nhưng thiếu ghi chú giải trình -> ném CashierShiftNoteRequiredException")
    void shouldThrowWhenDiscrepancyWithoutNotes() {
        Instant p1Time = Instant.parse("2026-09-21T02:00:00Z");
        Payment p1 = createPayment(new BigDecimal("500000.00"), PaymentMethod.CASH, p1Time);

        when(paymentRepository.findUnsettledByCashierForUpdate(eq(cashierId), any()))
                .thenReturn(List.of(p1));
        when(shiftCodeGenerator.generate()).thenReturn("CS000003");

        CloseCashierShiftCommand command = new CloseCashierShiftCommand(
                new BigDecimal("450000.00"),
                ""
        );

        assertThrows(CashierShiftNoteRequiredException.class, () -> service.close(command));
    }

    @Test
    @DisplayName("Validate số tiền mặt thực tế âm ném ValidationException")
    void shouldThrowWhenActualCashAmountIsNegative() {
        CloseCashierShiftCommand command = new CloseCashierShiftCommand(
                new BigDecimal("-1000.00"),
                "Notes"
        );

        assertThrows(ValidationException.class, () -> service.close(command));
    }

    @Test
    @DisplayName("P2-01: Ghi chú chứa ký tự đặc biệt được serialize thành JSON hợp lệ trong AuditLog")
    void shouldEscapeSpecialCharactersInAuditLogDetail() throws Exception {
        Instant p1Time = Instant.parse("2026-09-21T02:00:00Z");
        Payment p1 = createPayment(new BigDecimal("500000.00"), PaymentMethod.CASH, p1Time);

        when(paymentRepository.findUnsettledByCashierForUpdate(eq(cashierId), any()))
                .thenReturn(List.of(p1));
        when(shiftCodeGenerator.generate()).thenReturn("CS000010");
        when(cashierShiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String rawNotes = "Lệch tiền do trả nhầm cho bệnh nhân \"Nguyễn Văn A\"\nTại quầy 2";
        CloseCashierShiftCommand command = new CloseCashierShiftCommand(
                new BigDecimal("450000.00"),
                rawNotes
        );

        service.close(command);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog log = captor.getValue();
        assertNotNull(log.getDetail());
        // Phải parse được JSON hợp lệ, không ném exception
        com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(log.getDetail());
        assertEquals("CS000010", node.get("shiftCode").asText());
        assertEquals(rawNotes, node.get("notes").asText());
    }

    @Test
    @DisplayName("P2-02: Ca có khoản thu bị REFUNDED được tính đúng dòng tiền ròng và gán shiftId")
    void shouldIncludeRefundedPaymentsInShiftSettlement() {
        Instant p1Time = Instant.parse("2026-09-21T02:00:00Z");
        Instant p2Time = Instant.parse("2026-09-21T03:00:00Z");

        Payment recorded = createPayment(new BigDecimal("500000.00"), PaymentMethod.CASH, p1Time);
        Payment refunded = Payment.restore(
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("200000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("200000.00"), new BigDecimal("200000.00"),
                PaymentMethod.CASH, PaymentStatus.REFUNDED, cashierId, p2Time, "Hoàn trả test", UUID.randomUUID(), p2Time, p2Time, null
        );

        when(paymentRepository.findUnsettledByCashierForUpdate(eq(cashierId), any()))
                .thenReturn(List.of(recorded, refunded));
        when(shiftCodeGenerator.generate()).thenReturn("CS000011");
        when(cashierShiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Tiền thực tế trong két = 500k - 200k = 300k
        CloseCashierShiftCommand command = new CloseCashierShiftCommand(
                new BigDecimal("300000.00"),
                null
        );

        CashierShiftResult result = service.close(command);

        assertEquals(new BigDecimal("300000.00"), result.systemCashAmount());
        assertEquals(new BigDecimal("300000.00"), result.actualCashAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.differenceAmount()));
        assertEquals(CashierShiftStatus.CONFIRMED, result.status());

        assertTrue(recorded.isSettled());
        assertTrue(refunded.isSettled());
        assertEquals(result.id(), recorded.getCashierShiftId());
        assertEquals(result.id(), refunded.getCashierShiftId());
    }

    @Test
    @DisplayName("P1: Người dùng không có quyền chốt ca bị từ chối và ghi nhận ACCESS_DENIED audit")
    void shouldThrowAccessDeniedAndAuditWhenNotAuthorizedToCloseShift() {
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasPermission("CASHIER_SHIFT_CREATE")).thenReturn(false);

        CloseCashierShiftCommand command = new CloseCashierShiftCommand(
                new BigDecimal("500000.00"),
                null
        );

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.close(command));
        verify(authorizationAuditService).recordCloseAccessDenied(eq(cashierId), any());
    }

    @Test
    @DisplayName("P1: Khi tiền hoàn trả vượt quá tiền thu (net âm), hệ thống tính đúng signed amount và không floor về 0")
    void shouldCalculateSignedAmountWhenRefundsExceedPayments() {
        Instant p1Time = Instant.parse("2026-09-21T02:00:00Z");
        Instant p2Time = Instant.parse("2026-09-21T03:00:00Z");

        Payment recorded = createPayment(new BigDecimal("1000000.00"), PaymentMethod.CASH, p1Time);
        Payment refunded = Payment.restore(
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1500000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1500000.00"), new BigDecimal("1500000.00"),
                PaymentMethod.CASH, PaymentStatus.REFUNDED, cashierId, p2Time, "Hoàn trả toàn bộ", UUID.randomUUID(), p2Time, p2Time, null
        );

        when(paymentRepository.findUnsettledByCashierForUpdate(eq(cashierId), any()))
                .thenReturn(List.of(recorded, refunded));
        when(shiftCodeGenerator.generate()).thenReturn("CS000012");
        when(cashierShiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Tiền thực tế trong két = 0; Hệ thống = 1.000.000 - 1.500.000 = -500.000
        // Chênh lệch = 0 - (-500.000) = +500.000 -> Có chênh lệch, yêu cầu ghi chú
        CloseCashierShiftCommand command = new CloseCashierShiftCommand(
                BigDecimal.ZERO,
                "Hoàn tiền phát sinh vượt thu trong ca"
        );

        CashierShiftResult result = service.close(command);

        assertEquals(new BigDecimal("-500000.00"), result.systemCashAmount());
        assertEquals(BigDecimal.ZERO, result.actualCashAmount());
        assertEquals(new BigDecimal("500000.00"), result.differenceAmount());
        assertEquals(CashierShiftStatus.PENDING_CONFIRMATION, result.status());
    }

    private Payment createPayment(BigDecimal amount, PaymentMethod method, Instant paidAt) {
        return Payment.restore(
                UUID.randomUUID(), UUID.randomUUID(),
                amount, BigDecimal.ZERO, BigDecimal.ZERO,
                amount, amount,
                method, PaymentStatus.RECORDED, cashierId, paidAt, null, null, null, paidAt, null
        );
    }
}
