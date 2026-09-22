package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.billing.CashierShift;
import com.benhsoan.domain.billing.enums.CashierShiftStatus;
import com.benhsoan.domain.billing.exception.CashierShiftAlreadyConfirmedException;
import com.benhsoan.domain.billing.exception.CashierShiftNotFoundException;
import com.benhsoan.port.dto.command.billing.ConfirmCashierShiftCommand;
import com.benhsoan.port.dto.result.CashierShiftResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.billing.CashierShiftRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
<<<<<<< HEAD
import static org.mockito.ArgumentMatchers.eq;
=======
>>>>>>> 49e54faef023bb919dce508eec3bf599f4759064
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmCashierShiftService Tests")
class ConfirmCashierShiftServiceTest {

    @Mock private CashierShiftRepository cashierShiftRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;
<<<<<<< HEAD
    @Mock private CashierShiftAuthorizationAuditService authorizationAuditService;
=======
>>>>>>> 49e54faef023bb919dce508eec3bf599f4759064

    private ConfirmCashierShiftService service;
    private CashierShiftResultMapper resultMapper;

    private final UUID managerId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-21T10:00:00Z");

    @BeforeEach
    void setUp() {
        resultMapper = new CashierShiftResultMapper(userRepository);
        service = new ConfirmCashierShiftService(
                cashierShiftRepository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                resultMapper,
<<<<<<< HEAD
                authorizationAuditService,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
        org.mockito.Mockito.lenient().when(currentUserPort.getCurrentUserId()).thenReturn(managerId);
=======
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
>>>>>>> 49e54faef023bb919dce508eec3bf599f4759064
    }

    @Test
    @DisplayName("Chỉ Quản lý phòng khám mới có quyền xác nhận phiếu chốt ca")
    void shouldThrowAccessDeniedWhenNotManager() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

<<<<<<< HEAD
        UUID shiftId = UUID.randomUUID();
        ConfirmCashierShiftCommand command = new ConfirmCashierShiftCommand(
                shiftId,
=======
        ConfirmCashierShiftCommand command = new ConfirmCashierShiftCommand(
                UUID.randomUUID(),
>>>>>>> 49e54faef023bb919dce508eec3bf599f4759064
                "Duyệt ca"
        );

        assertThrows(AccessDeniedException.class, () -> service.confirm(command));
<<<<<<< HEAD
        verify(authorizationAuditService).recordConfirmAccessDenied(eq(managerId), eq(shiftId), any());
=======
>>>>>>> 49e54faef023bb919dce508eec3bf599f4759064
    }

    @Test
    @DisplayName("Ném CashierShiftNotFoundException khi không tìm thấy phiếu")
    void shouldThrowWhenShiftNotFound() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(managerId);
        when(clockPort.now()).thenReturn(now);

        UUID shiftId = UUID.randomUUID();
        when(cashierShiftRepository.findByIdForUpdate(shiftId)).thenReturn(Optional.empty());

        ConfirmCashierShiftCommand command = new ConfirmCashierShiftCommand(
                shiftId,
                "Duyệt ca"
        );

        assertThrows(CashierShiftNotFoundException.class, () -> service.confirm(command));
    }

    @Test
    @DisplayName("Quản lý xác nhận ca chốt thành công, cập nhật trạng thái CONFIRMED và ghi AuditLog")
    void shouldConfirmShiftSuccessfully() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(managerId);
        when(clockPort.now()).thenReturn(now);

        UUID shiftId = UUID.randomUUID();
        Instant startTime = Instant.parse("2026-09-21T01:00:00Z");
        CashierShift shift = CashierShift.create(
                shiftId,
                "CS000001",
                UUID.randomUUID(),
                startTime,
                startTime.plusSeconds(3600),
                1,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("90000.00"),
                "Lệch 10k",
                startTime.plusSeconds(3600)
        );

        assertEquals(CashierShiftStatus.PENDING_CONFIRMATION, shift.getStatus());

        when(cashierShiftRepository.findByIdForUpdate(shiftId)).thenReturn(Optional.of(shift));
        when(cashierShiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfirmCashierShiftCommand command = new ConfirmCashierShiftCommand(
                shiftId,
                "Đã đối soát sổ quỹ và duyệt"
        );

        CashierShiftResult result = service.confirm(command);

        assertEquals(CashierShiftStatus.CONFIRMED, result.status());
        assertEquals(managerId, result.confirmedBy());
        assertEquals(now, result.confirmedAt());
        assertEquals("Đã đối soát sổ quỹ và duyệt", result.confirmationNotes());

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Ném CashierShiftAlreadyConfirmedException khi xác nhận ca đã CONFIRMED")
    void shouldThrowWhenShiftAlreadyConfirmed() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(managerId);
        when(clockPort.now()).thenReturn(now);

        UUID shiftId = UUID.randomUUID();
        Instant startTime = Instant.parse("2026-09-21T01:00:00Z");
        CashierShift shift = CashierShift.create(
                shiftId,
                "CS000002",
                UUID.randomUUID(),
                startTime,
                startTime.plusSeconds(3600),
                1,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                null,
                startTime.plusSeconds(3600)
        );

        // Khớp quỹ -> đã CONFIRMED
        assertEquals(CashierShiftStatus.CONFIRMED, shift.getStatus());

        when(cashierShiftRepository.findByIdForUpdate(shiftId)).thenReturn(Optional.of(shift));

        ConfirmCashierShiftCommand command = new ConfirmCashierShiftCommand(
                shiftId,
                "Duyệt lại"
        );

        assertThrows(CashierShiftAlreadyConfirmedException.class, () -> service.confirm(command));
    }

    @Test
    @DisplayName("P3-02: Quản lý không được tự xác nhận phiếu chốt ca lệch quỹ của chính mình")
    void shouldPreventManagerFromConfirmingOwnShift() {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(managerId);
        when(clockPort.now()).thenReturn(now);

        UUID shiftId = UUID.randomUUID();
        Instant startTime = Instant.parse("2026-09-21T01:00:00Z");
        CashierShift shift = CashierShift.create(
                shiftId,
                "CS000003",
                managerId, // Ca do chính manager lập!
                startTime,
                startTime.plusSeconds(3600),
                1,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("80000.00"),
                "Lệch 20k",
                startTime.plusSeconds(3600)
        );

        when(cashierShiftRepository.findByIdForUpdate(shiftId)).thenReturn(Optional.of(shift));

        ConfirmCashierShiftCommand command = new ConfirmCashierShiftCommand(
                shiftId,
                "Tự duyệt ca của mình"
        );

        assertThrows(com.benhsoan.domain.billing.exception.SelfConfirmationNotAllowedException.class,
                () -> service.confirm(command));
<<<<<<< HEAD
        verify(authorizationAuditService).recordConfirmAccessDenied(eq(managerId), eq(shiftId), any());
=======
>>>>>>> 49e54faef023bb919dce508eec3bf599f4759064
    }

    @Test
    @DisplayName("P2-01: Ghi chú duyệt có ký tự đặc biệt được serialize thành JSON hợp lệ trong AuditLog")
    void shouldEscapeSpecialCharactersInConfirmationAuditLogDetail() throws Exception {
        when(currentUserPort.hasRole("MANAGER")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(managerId);
        when(clockPort.now()).thenReturn(now);

        UUID shiftId = UUID.randomUUID();
        Instant startTime = Instant.parse("2026-09-21T01:00:00Z");
        CashierShift shift = CashierShift.create(
                shiftId,
                "CS000004",
                UUID.randomUUID(),
                startTime,
                startTime.plusSeconds(3600),
                1,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("90000.00"),
                "Lệch 10k",
                startTime.plusSeconds(3600)
        );

        when(cashierShiftRepository.findByIdForUpdate(shiftId)).thenReturn(Optional.of(shift));
        when(cashierShiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String rawConfirmationNotes = "Đồng ý duyệt lệch tiền cho thu ngân: \"Nguyễn Thị B\"\nLý do: Đã kiểm tra";
        ConfirmCashierShiftCommand command = new ConfirmCashierShiftCommand(
                shiftId,
                rawConfirmationNotes
        );

        service.confirm(command);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog log = captor.getValue();
        com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(log.getDetail());
        assertEquals("CS000004", node.get("shiftCode").asText());
        assertEquals(rawConfirmationNotes, node.get("confirmationNotes").asText());
    }
}
