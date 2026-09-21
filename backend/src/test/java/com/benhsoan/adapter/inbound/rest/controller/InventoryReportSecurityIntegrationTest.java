package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.InventoryReportRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockExportResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockItemResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockReportResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockSummaryResult;
import com.benhsoan.port.inbound.inventory.ExportInventoryInOutStockReportUseCase;
import com.benhsoan.port.inbound.inventory.GetInventoryInOutStockReportUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = InventoryReportController.class)
@Import({
        AopAutoConfiguration.class,
        InventoryReportSecurityIntegrationTest.AspectTestConfig.class,
        InventoryReportRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
@DisplayName("Inventory Report Security Integration Tests (NCL-06-CN-013)")
class InventoryReportSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetInventoryInOutStockReportUseCase getInventoryInOutStockReportUseCase;

    @MockitoBean
    private ExportInventoryInOutStockReportUseCase exportInventoryInOutStockReportUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private ClockPort clockPort;

    @Test
    @DisplayName("Allows pharmacist with PHARMACY_READ to view inventory report")
    void allowsPharmacistToViewReport() throws Exception {
        when(getInventoryInOutStockReportUseCase.getReport(any())).thenReturn(createDummyReportResult());

        mockMvc.perform(get("/inventory/reports/in-out-stock")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("pharmacist").roles("PHARMACIST")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_READ"))))
                .andExpect(status().isOk());

        verify(getInventoryInOutStockReportUseCase).getReport(any());
    }

    @Test
    @DisplayName("Allows clinic manager with REPORT_VIEW to view inventory report")
    void allowsManagerToViewReport() throws Exception {
        when(getInventoryInOutStockReportUseCase.getReport(any())).thenReturn(createDummyReportResult());

        mockMvc.perform(get("/inventory/reports/in-out-stock")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("manager").roles("MANAGER")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_REPORT_VIEW"))))
                .andExpect(status().isOk());

        verify(getInventoryInOutStockReportUseCase).getReport(any());
    }

    @Test
    @DisplayName("Allows pharmacist with PHARMACY_READ to export inventory report")
    void allowsPharmacistToExportReport() throws Exception {
        when(exportInventoryInOutStockReportUseCase.export(any())).thenReturn(new InventoryInOutStockExportResult(
                "report.csv", "text/csv; charset=UTF-8", "content".getBytes(StandardCharsets.UTF_8)
        ));

        mockMvc.perform(get("/inventory/reports/in-out-stock/export")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("pharmacist").roles("PHARMACIST")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_READ"))))
                .andExpect(status().isOk());

        verify(exportInventoryInOutStockReportUseCase).export(any());
    }

    @Test
    @DisplayName("Allows clinic manager with REPORT_EXPORT to export inventory report")
    void allowsManagerToExportReport() throws Exception {
        when(exportInventoryInOutStockReportUseCase.export(any())).thenReturn(new InventoryInOutStockExportResult(
                "report.csv", "text/csv; charset=UTF-8", "content".getBytes(StandardCharsets.UTF_8)
        ));

        mockMvc.perform(get("/inventory/reports/in-out-stock/export")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("manager").roles("MANAGER")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_REPORT_EXPORT"))))
                .andExpect(status().isOk());

        verify(exportInventoryInOutStockReportUseCase).export(any());
    }

    @Test
    @DisplayName("Forbids receptionist without pharmacy/report permission and audits denied attempt")
    void forbidsReceptionistFromViewingReport() throws Exception {
        UUID receptionistId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);

        mockMvc.perform(get("/inventory/reports/in-out-stock")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("receptionist").roles("RECEPTIONIST")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getInventoryInOutStockReportUseCase);
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Forbids doctor when authorizer denies access (contextual role restriction VT-02)")
    void forbidsDoctorWhenAuthorizerDeniesAccess() throws Exception {
        when(getInventoryInOutStockReportUseCase.getReport(any()))
                .thenThrow(new AccessDeniedException("Chỉ dược sĩ, quản lý phòng khám hoặc quản trị viên mới có quyền truy cập báo cáo kho."));

        mockMvc.perform(get("/inventory/reports/in-out-stock")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("doctor").roles("DOCTOR")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_READ"))))
                .andExpect(status().isForbidden());
    }

    private static InventoryInOutStockReportResult createDummyReportResult() {
        return new InventoryInOutStockReportResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                Instant.parse("2026-09-21T10:00:00Z"),
                true,
                List.of(new InventoryInOutStockItemResult(
                        UUID.randomUUID(), "MED-001", "Paracetamol", "Viên", 10, 20, 5, 0, 0, 25
                )),
                new InventoryInOutStockSummaryResult(1, 10, 20, 5, 0, 0, 25)
        );
    }
}
