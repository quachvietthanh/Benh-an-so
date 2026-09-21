package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.InventoryRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.BatchAdjustmentResult;
import com.benhsoan.port.dto.result.DiscardBatchResult;
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
@Import({ InventoryRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class,
                RequirePermissionAspect.class, PermissionEvaluator.class,
                InventoryAdjustmentSecurityIntegrationTest.AspectTestConfig.class })
@DisplayName("Inventory Adjustment & Discard Security Integration Tests (TC-04)")
class InventoryAdjustmentSecurityIntegrationTest {

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
        private AdjustBatchStockUseCase adjustBatchStockUseCase;
        @MockitoBean
        private DiscardExpiredBatchUseCase discardExpiredBatchUseCase;
        @MockitoBean
        private GetInventoryStockReportUseCase getInventoryStockReportUseCase;
        @MockitoBean
        private ExportInventoryStockReportUseCase exportInventoryStockReportUseCase;
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
        @DisplayName("Allows pharmacist with PHARMACY_UPDATE to adjust batch stock")
        void allowsPharmacistToAdjustStock() throws Exception {
                UUID batchId = UUID.randomUUID();
                when(adjustBatchStockUseCase.adjustStock(any())).thenReturn(new BatchAdjustmentResult(
                                batchId, UUID.randomUUID(), "TH001", "Paracetamol", "BATCH-01",
                                LocalDate.of(2027, 1, 1), 100, 85, -15, BatchStatus.ACTIVE,
                                "Kiểm kê", UUID.randomUUID(), Instant.now()));

                mockMvc.perform(post("/inventory/batches/{id}/adjust", batchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"actualQuantity\": 85, \"reason\": \"Kiểm kê\"}")
                                .with(user("pharmacist")
                                                .authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_UPDATE"))))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Allows pharmacist with PHARMACY_UPDATE to discard expired batch")
        void allowsPharmacistToDiscardExpiredBatch() throws Exception {
                UUID batchId = UUID.randomUUID();
                when(discardExpiredBatchUseCase.discardExpired(any())).thenReturn(new DiscardBatchResult(
                                batchId, UUID.randomUUID(), "TH001", "Paracetamol", "BATCH-01",
                                LocalDate.of(2026, 9, 1), 50, BatchStatus.EXPIRED,
                                "Hủy hết hạn", UUID.randomUUID(), Instant.now()));

                mockMvc.perform(post("/inventory/batches/{id}/discard", batchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\": \"Hủy hết hạn\"}")
                                .with(user("pharmacist")
                                                .authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_UPDATE"))))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC-04: Forbids receptionist and writes ACCESS_DENIED audit log when attempting to adjust inventory")
        void forbidsReceptionistFromAdjustingStockAndAuditsDeniedAttempt() throws Exception {
                UUID batchId = UUID.randomUUID();
                UUID receptionistId = UUID.randomUUID();
                when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);

                mockMvc.perform(post("/inventory/batches/{id}/adjust", batchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"actualQuantity\": 85, \"reason\": \"Lễ tân thử chỉnh kho\"}")
                                .with(user("receptionist")
                                                .authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                                .andExpect(status().isForbidden());

                verify(auditLogRepository)
                                .save(argThat((AuditLog log) -> log.getActionType() == ActionType.ACCESS_DENIED
                                                && log.getResourceType() == ResourceType.PERMISSION));
        }

        @Test
        @DisplayName("TC-04: Forbids receptionist and writes ACCESS_DENIED audit log when attempting to discard expired batch")
        void forbidsReceptionistFromDiscardingBatchAndAuditsDeniedAttempt() throws Exception {
                UUID batchId = UUID.randomUUID();
                UUID receptionistId = UUID.randomUUID();
                when(currentUserPort.getCurrentUserId()).thenReturn(receptionistId);

                mockMvc.perform(post("/inventory/batches/{id}/discard", batchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\": \"Lễ tân thử hủy lô\"}")
                                .with(user("receptionist")
                                                .authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                                .andExpect(status().isForbidden());

                verify(auditLogRepository)
                                .save(argThat((AuditLog log) -> log.getActionType() == ActionType.ACCESS_DENIED
                                                && log.getResourceType() == ResourceType.PERMISSION));
        }

        @Test
        @DisplayName("Forbids doctor (with only PHARMACY_READ) from adjusting inventory")
        void forbidsDoctorFromAdjustingStock() throws Exception {
                UUID batchId = UUID.randomUUID();

                mockMvc.perform(post("/inventory/batches/{id}/adjust", batchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"actualQuantity\": 85, \"reason\": \"Bác sĩ thử chỉnh kho\"}")
                                .with(user("doctor")
                                                .authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_READ"))))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Forbids doctor (with only PHARMACY_READ) from discarding expired batch")
        void forbidsDoctorFromDiscardingBatch() throws Exception {
                UUID batchId = UUID.randomUUID();

                mockMvc.perform(post("/inventory/batches/{id}/discard", batchId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\": \"Bác sĩ thử hủy lô\"}")
                                .with(user("doctor")
                                                .authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_READ"))))
                                .andExpect(status().isForbidden());
        }
}
