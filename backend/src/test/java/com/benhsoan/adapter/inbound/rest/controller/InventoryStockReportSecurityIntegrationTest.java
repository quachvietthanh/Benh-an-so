package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.InventoryRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.InventoryStockReportExportResult;
import com.benhsoan.port.dto.result.InventoryStockReportResult;
import com.benhsoan.port.inbound.inventory.AdjustBatchStockUseCase;
import com.benhsoan.port.inbound.inventory.DiscardExpiredBatchUseCase;
import com.benhsoan.port.inbound.inventory.ExportInventoryStockReportUseCase;
import com.benhsoan.port.inbound.inventory.GetInventoryStockReportUseCase;
import com.benhsoan.port.inbound.inventory.ListInventoryBatchesUseCase;
import com.benhsoan.port.inbound.inventory.ListInventoryExpiryAlertsUseCase;
import com.benhsoan.port.inbound.inventory.ListInventoryStocksUseCase;
import com.benhsoan.port.inbound.inventory.ListLowStockMedicinesUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = InventoryController.class)
@Import({InventoryRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class,
        RequirePermissionAspect.class, PermissionEvaluator.class, InventoryStockReportSecurityIntegrationTest.AspectTestConfig.class})
class InventoryStockReportSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListInventoryStocksUseCase listInventoryStocksUseCase;
    @MockitoBean
    private ListInventoryBatchesUseCase listInventoryBatchesUseCase;
    @MockitoBean
    private ListInventoryExpiryAlertsUseCase listInventoryExpiryAlertsUseCase;
    @MockitoBean
    private ListLowStockMedicinesUseCase listLowStockMedicinesUseCase;
    @MockitoBean
    private GetInventoryStockReportUseCase getInventoryStockReportUseCase;
    @MockitoBean
    private ExportInventoryStockReportUseCase exportInventoryStockReportUseCase;
    @MockitoBean
    private AdjustBatchStockUseCase adjustBatchStockUseCase;
    @MockitoBean
    private DiscardExpiredBatchUseCase discardExpiredBatchUseCase;
    @MockitoBean
    private JwtTokenPort jwtTokenPort;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserSessionRepository userSessionRepository;
    @MockitoBean
    private ClockPort clockPort;
    @MockitoBean
    private RoleRepository roleRepository;
    @MockitoBean
    private AuditLogRepository auditLogRepository;
    @MockitoBean
    private CurrentUserPort currentUserPort;

    @Test
    void pharmacistWithInventoryReportViewCanViewStockReport() throws Exception {
        when(getInventoryStockReportUseCase.getStockReport(any(), any()))
                .thenReturn(emptyReport());

        mockMvc.perform(get("/inventory/report/stock-in-out")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_INVENTORY_REPORT_VIEW"))))
                .andExpect(status().isOk());
    }

    @Test
    void managerWithInventoryReportViewCanViewStockReport() throws Exception {
        when(getInventoryStockReportUseCase.getStockReport(any(), any()))
                .thenReturn(emptyReport());

        mockMvc.perform(get("/inventory/report/stock-in-out")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("manager").authorities(new SimpleGrantedAuthority("PERMISSION_INVENTORY_REPORT_VIEW"))))
                .andExpect(status().isOk());
    }

    @Test
    void adminWithInventoryReportViewCanViewStockReport() throws Exception {
        when(getInventoryStockReportUseCase.getStockReport(any(), any()))
                .thenReturn(emptyReport());

        mockMvc.perform(get("/inventory/report/stock-in-out")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_INVENTORY_REPORT_VIEW"))))
                .andExpect(status().isOk());
    }

    @Test
    void doctorWithPharmacyReadButWithoutInventoryReportViewIsForbidden() throws Exception {
        mockMvc.perform(get("/inventory/report/stock-in-out")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void receptionistWithoutPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/inventory/report/stock-in-out")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/inventory/report/stock-in-out")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pharmacistCanExportStockReport() throws Exception {
        when(exportInventoryStockReportUseCase.export(any(), any()))
                .thenReturn(new InventoryStockReportExportResult(
                        "stock-in-out-report.csv",
                        "text/csv; charset=UTF-8",
                        new byte[0]));

        mockMvc.perform(get("/inventory/report/stock-in-out/export")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_INVENTORY_REPORT_VIEW"))))
                .andExpect(status().isOk());
    }

    @Test
    void doctorCannotExportStockReport() throws Exception {
        mockMvc.perform(get("/inventory/report/stock-in-out/export")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31")
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestCannotExportStockReport() throws Exception {
        mockMvc.perform(get("/inventory/report/stock-in-out/export")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-31"))
                .andExpect(status().isUnauthorized());
    }

    private InventoryStockReportResult emptyReport() {
        return new InventoryStockReportResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                Instant.parse("2026-08-31T08:00:00Z"),
                false,
                List.of()
        );
    }
}
