package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.InventoryRestMapper;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.inventory.enums.InventoryExpiryAlertStatus;
import com.benhsoan.port.dto.result.InventoryBatchResult;
import com.benhsoan.port.dto.result.InventoryExpiryAlertResult;
import com.benhsoan.port.dto.result.InventoryStockResult;
import com.benhsoan.port.dto.result.LowStockMedicineResult;
import com.benhsoan.port.inbound.inventory.AdjustBatchStockUseCase;
import com.benhsoan.port.inbound.inventory.DiscardExpiredBatchUseCase;
import com.benhsoan.port.inbound.inventory.GetInventoryStockReportUseCase;
import com.benhsoan.port.inbound.inventory.ListInventoryBatchesUseCase;
import com.benhsoan.port.inbound.inventory.ListInventoryExpiryAlertsUseCase;
import com.benhsoan.port.inbound.inventory.ListLowStockMedicinesUseCase;
import com.benhsoan.port.inbound.inventory.ListInventoryStocksUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(InventoryRestMapper.class)
@DisplayName("InventoryController - MockMvc Tests")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListInventoryStocksUseCase listInventoryStocksUseCase;

    @MockitoBean
    private ListInventoryBatchesUseCase listInventoryBatchesUseCase;

    @MockitoBean
    private ListLowStockMedicinesUseCase listLowStockMedicinesUseCase;

    @MockitoBean
    private ListInventoryExpiryAlertsUseCase listInventoryExpiryAlertsUseCase;

    @MockitoBean
    private GetInventoryStockReportUseCase getInventoryStockReportUseCase;

    @MockitoBean
    private AdjustBatchStockUseCase adjustBatchStockUseCase;

    @MockitoBean
    private DiscardExpiredBatchUseCase discardExpiredBatchUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private ClockPort clockPort;

    @Test
    @DisplayName("GET /inventory/stocks returns stock summary")
    void listStocksReturnsSummary() throws Exception {
        UUID medicineId = UUID.randomUUID();

        when(listInventoryStocksUseCase.list(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new InventoryStockResult(
                        medicineId,
                        "MED-001",
                        "Paracetamol",
                        "Paracetamol",
                        "500 mg",
                        "vien",
                        true,
                        120,
                        95,
                        2,
                        LocalDate.of(2026, 9, 1)
                )));

        mockMvc.perform(get("/inventory/stocks")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicineId").value(medicineId.toString()))
                .andExpect(jsonPath("$[0].stockQuantity").value(120))
                .andExpect(jsonPath("$[0].eligibleStockQuantity").value(95))
                .andExpect(jsonPath("$[0].activeBatchCount").value(2))
                .andExpect(jsonPath("$[0].nearestExpiryDate").value("2026-09-01"));
    }

    @Test
    @DisplayName("GET /inventory/batches returns batch detail")
    void listBatchesReturnsBatchDetail() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        when(listInventoryBatchesUseCase.list(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new InventoryBatchResult(
                        batchId,
                        medicineId,
                        "MED-001",
                        "Paracetamol",
                        "BATCH-001",
                        LocalDate.of(2026, 12, 31),
                        40,
                        BatchStatus.ACTIVE,
                        true,
                        Instant.parse("2026-08-01T00:00:00Z"),
                        Instant.parse("2026-08-07T00:00:00Z")
                )));

        mockMvc.perform(get("/inventory/batches")
                        .param("medicineId", medicineId.toString())
                        .param("status", "ACTIVE")
                        .param("eligibleForDispense", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchId").value(batchId.toString()))
                .andExpect(jsonPath("$[0].medicineId").value(medicineId.toString()))
                .andExpect(jsonPath("$[0].batchNumber").value("BATCH-001"))
                .andExpect(jsonPath("$[0].quantity").value(40))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].eligibleForDispense").value(true));
    }

    @Test
    @DisplayName("GET /inventory/low-stock returns low stock alerts")
    void listLowStockReturnsAlerts() throws Exception {
        UUID medicineId = UUID.randomUUID();

        when(listLowStockMedicinesUseCase.list())
                .thenReturn(List.of(new LowStockMedicineResult(
                        medicineId,
                        "MED-LOW-001",
                        "Metformin 500 mg",
                        "vien",
                        80,
                        15,
                        40,
                        25
                )));

        mockMvc.perform(get("/inventory/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicineId").value(medicineId.toString()))
                .andExpect(jsonPath("$[0].medicineCode").value("MED-LOW-001"))
                .andExpect(jsonPath("$[0].eligibleStockQuantity").value(15))
                .andExpect(jsonPath("$[0].minStockThreshold").value(40))
                .andExpect(jsonPath("$[0].shortageQuantity").value(25));
    }

    @Test
    @DisplayName("GET /inventory/low-stock returns empty array when no alerts")
    void listLowStockReturnsEmptyArray() throws Exception {
        when(listLowStockMedicinesUseCase.list()).thenReturn(List.of());

        mockMvc.perform(get("/inventory/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /inventory/expiry-alerts returns near expiry and expired batches")
    void listExpiryAlertsReturnsAlerts() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();

        when(listInventoryExpiryAlertsUseCase.list(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new InventoryExpiryAlertResult(
                        batchId,
                        medicineId,
                        "MED-EXP-001",
                        "Paracetamol",
                        "BATCH-EXP-001",
                        LocalDate.of(2026, 8, 20),
                        15,
                        BatchStatus.ACTIVE,
                        9,
                        InventoryExpiryAlertStatus.NEAR_EXPIRY,
                        Instant.parse("2026-08-01T00:00:00Z"),
                        Instant.parse("2026-08-07T00:00:00Z")
                )));

        mockMvc.perform(get("/inventory/expiry-alerts")
                        .param("medicineId", medicineId.toString())
                        .param("status", "NEAR_EXPIRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchId").value(batchId.toString()))
                .andExpect(jsonPath("$[0].medicineId").value(medicineId.toString()))
                .andExpect(jsonPath("$[0].batchNumber").value("BATCH-EXP-001"))
                .andExpect(jsonPath("$[0].daysToExpiry").value(9))
                .andExpect(jsonPath("$[0].alertStatus").value("NEAR_EXPIRY"));
    }

    @Test
    @DisplayName("POST /inventory/batches/{id}/adjust returns 200 with adjustment result")
    void adjustBatchStockReturns200WhenValid() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T08:30:00Z");

        when(adjustBatchStockUseCase.adjustStock(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.benhsoan.port.dto.result.BatchAdjustmentResult(
                        batchId,
                        medicineId,
                        "TH001",
                        "Paracetamol 500mg",
                        "BATCH-001",
                        LocalDate.of(2027, 12, 31),
                        100,
                        85,
                        -15,
                        BatchStatus.ACTIVE,
                        "Kiểm kê hao hụt 15 viên",
                        userId,
                        now
                ));

        String requestBody = """
                {
                    "actualQuantity": 85,
                    "reason": "Kiểm kê hao hụt 15 viên"
                }
                """;

        mockMvc.perform(post("/inventory/batches/{id}/adjust", batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.medicineCode").value("TH001"))
                .andExpect(jsonPath("$.quantityBefore").value(100))
                .andExpect(jsonPath("$.quantityAfter").value(85))
                .andExpect(jsonPath("$.quantityChange").value(-15))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.reason").value("Kiểm kê hao hụt 15 viên"));
    }

    @Test
    @DisplayName("POST /inventory/batches/{id}/adjust returns 400 when reason is blank (QTN-32)")
    void adjustBatchStockReturns400WhenReasonBlank() throws Exception {
        UUID batchId = UUID.randomUUID();
        String requestBody = """
                {
                    "actualQuantity": 85,
                    "reason": "   "
                }
                """;

        mockMvc.perform(post("/inventory/batches/{id}/adjust", batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /inventory/batches/{id}/adjust returns 400 when actualQuantity is negative")
    void adjustBatchStockReturns400WhenActualQuantityNegative() throws Exception {
        UUID batchId = UUID.randomUUID();
        String requestBody = """
                {
                    "actualQuantity": -5,
                    "reason": "Lý do hợp lệ"
                }
                """;

        mockMvc.perform(post("/inventory/batches/{id}/adjust", batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /inventory/batches/{id}/discard returns 200 with discard result")
    void discardExpiredBatchReturns200WhenValid() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T08:35:00Z");

        when(discardExpiredBatchUseCase.discardExpired(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.benhsoan.port.dto.result.DiscardBatchResult(
                        batchId,
                        medicineId,
                        "TH001",
                        "Paracetamol 500mg",
                        "BATCH-001",
                        LocalDate.of(2026, 9, 15),
                        50,
                        BatchStatus.EXPIRED,
                        "Hủy lô hết hạn 15/09/2026",
                        userId,
                        now
                ));

        String requestBody = """
                {
                    "reason": "Hủy lô hết hạn 15/09/2026"
                }
                """;

        mockMvc.perform(post("/inventory/batches/{id}/discard", batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.discardedQuantity").value(50))
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.reason").value("Hủy lô hết hạn 15/09/2026"));
    }

    @Test
    @DisplayName("POST /inventory/batches/{id}/discard returns 400 when reason is blank (QTN-32)")
    void discardExpiredBatchReturns400WhenReasonBlank() throws Exception {
        UUID batchId = UUID.randomUUID();
        String requestBody = """
                {
                    "reason": ""
                }
                """;

        mockMvc.perform(post("/inventory/batches/{id}/discard", batchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
