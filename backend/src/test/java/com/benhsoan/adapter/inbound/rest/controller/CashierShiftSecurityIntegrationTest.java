package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.CashierShiftRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.billing.enums.CashierShiftStatus;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.CashierShiftResult;
import com.benhsoan.port.dto.result.CurrentShiftSummaryResult;
import com.benhsoan.port.inbound.billing.CloseCashierShiftUseCase;
import com.benhsoan.port.inbound.billing.ConfirmCashierShiftUseCase;
import com.benhsoan.port.inbound.billing.GetCashierShiftByIdUseCase;
import com.benhsoan.port.inbound.billing.GetCurrentShiftSummaryUseCase;
import com.benhsoan.port.inbound.billing.SearchCashierShiftsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = CashierShiftController.class)
@Import({
        AnonymizationModeState.class,
        CashierShiftRestMapper.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        CashierShiftSecurityIntegrationTest.AspectTestConfig.class
})
@DisplayName("CashierShiftController Security Integration Tests")
class CashierShiftSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetCurrentShiftSummaryUseCase getCurrentShiftSummaryUseCase;
    @MockitoBean private CloseCashierShiftUseCase closeCashierShiftUseCase;
    @MockitoBean private ConfirmCashierShiftUseCase confirmCashierShiftUseCase;
    @MockitoBean private SearchCashierShiftsUseCase searchCashierShiftsUseCase;
    @MockitoBean private GetCashierShiftByIdUseCase getCashierShiftByIdUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void allowsUsersWithCashierShiftReadPermissionToAccessReadEndpoints() throws Exception {
        UUID cashierId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();

        when(getCurrentShiftSummaryUseCase.getCurrentSummary()).thenReturn(createSampleSummaryResult(cashierId));
        when(searchCashierShiftsUseCase.search(any())).thenReturn(Page.empty());
        when(getCashierShiftByIdUseCase.getById(shiftId)).thenReturn(
                createSampleShiftResult(shiftId, cashierId, CashierShiftStatus.CONFIRMED, new BigDecimal("3000000"), BigDecimal.ZERO)
        );

        mockMvc.perform(get("/cashier-shifts/current-summary")
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_CASHIER_SHIFT_READ"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cashier-shifts")
                        .with(user("manager").authorities(new SimpleGrantedAuthority("PERMISSION_CASHIER_SHIFT_READ"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cashier-shifts/{shiftId}", shiftId)
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_CASHIER_SHIFT_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    void deniesUsersWithoutCashierShiftReadPermission() throws Exception {
        UUID shiftId = UUID.randomUUID();

        mockMvc.perform(get("/cashier-shifts/current-summary")
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/cashier-shifts")
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/cashier-shifts/{shiftId}", shiftId)
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_MEDICINE_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsUsersWithCashierShiftCreatePermissionToCloseShift() throws Exception {
        UUID shiftId = UUID.randomUUID();
        UUID cashierId = UUID.randomUUID();

        when(closeCashierShiftUseCase.close(any())).thenReturn(
                createSampleShiftResult(shiftId, cashierId, CashierShiftStatus.CONFIRMED, new BigDecimal("3000000"), BigDecimal.ZERO)
        );

        mockMvc.perform(post("/cashier-shifts/close")
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_CASHIER_SHIFT_CREATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualCashAmount\":3000000,\"notes\":\"Đủ tiền\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void deniesUsersWithoutCashierShiftCreatePermissionToCloseShift() throws Exception {
        mockMvc.perform(post("/cashier-shifts/close")
                        .with(user("manager").authorities(new SimpleGrantedAuthority("PERMISSION_CASHIER_SHIFT_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualCashAmount\":3000000,\"notes\":\"Đủ tiền\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/cashier-shifts/close")
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_CLINICAL_ORDER_CREATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualCashAmount\":3000000,\"notes\":\"Đủ tiền\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsUsersWithCashierShiftConfirmPermissionToConfirmShift() throws Exception {
        UUID shiftId = UUID.randomUUID();
        UUID cashierId = UUID.randomUUID();

        when(confirmCashierShiftUseCase.confirm(any())).thenReturn(
                createSampleShiftResult(shiftId, cashierId, CashierShiftStatus.CONFIRMED, new BigDecimal("2900000"), new BigDecimal("-100000"))
        );

        mockMvc.perform(post("/cashier-shifts/{shiftId}/confirm", shiftId)
                        .with(user("manager").authorities(new SimpleGrantedAuthority("PERMISSION_CASHIER_SHIFT_CONFIRM")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationNotes\":\"Đã kiểm tra chênh lệch và đồng ý xác nhận\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deniesUsersWithoutCashierShiftConfirmPermissionToConfirmShift() throws Exception {
        UUID shiftId = UUID.randomUUID();

        mockMvc.perform(post("/cashier-shifts/{shiftId}/confirm", shiftId)
                        .with(user("receptionist").authorities(
                                new SimpleGrantedAuthority("PERMISSION_CASHIER_SHIFT_READ"),
                                new SimpleGrantedAuthority("PERMISSION_CASHIER_SHIFT_CREATE")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationNotes\":\"Duyệt\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deniesUnauthenticatedAccess() throws Exception {
        UUID shiftId = UUID.randomUUID();

        mockMvc.perform(get("/cashier-shifts/current-summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/cashier-shifts/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualCashAmount\":1000000}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/cashier-shifts/{shiftId}/confirm", shiftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationNotes\":\"Duyệt\"}"))
                .andExpect(status().isUnauthorized());
    }

    private CashierShiftResult createSampleShiftResult(
            UUID shiftId,
            UUID cashierId,
            CashierShiftStatus status,
            BigDecimal actualCash,
            BigDecimal diff
    ) {
        return new CashierShiftResult(
                shiftId,
                "CS2609210001",
                cashierId,
                "Lễ tân Nguyễn",
                Instant.parse("2026-09-21T01:00:00Z"),
                Instant.parse("2026-09-21T08:00:00Z"),
                10,
                new BigDecimal("5000000.00"),
                new BigDecimal("3000000.00"),
                new BigDecimal("2000000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                actualCash,
                diff,
                status,
                diff.compareTo(BigDecimal.ZERO) != 0 ? "Lệch tiền" : null,
                status == CashierShiftStatus.CONFIRMED ? UUID.randomUUID() : null,
                status == CashierShiftStatus.CONFIRMED ? "Quản lý duyệt" : null,
                status == CashierShiftStatus.CONFIRMED ? Instant.parse("2026-09-21T08:05:00Z") : null,
                status == CashierShiftStatus.CONFIRMED ? "Đã xác nhận" : null,
                Instant.parse("2026-09-21T08:00:00Z")
        );
    }

    private CurrentShiftSummaryResult createSampleSummaryResult(UUID cashierId) {
        return new CurrentShiftSummaryResult(
                cashierId,
                "Lễ tân Nguyễn",
                Instant.parse("2026-09-21T01:00:00Z"),
                Instant.parse("2026-09-21T08:00:00Z"),
                10,
                new BigDecimal("3000000.00"),
                new BigDecimal("2000000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("5000000.00"),
                List.of(UUID.randomUUID())
        );
    }
}
