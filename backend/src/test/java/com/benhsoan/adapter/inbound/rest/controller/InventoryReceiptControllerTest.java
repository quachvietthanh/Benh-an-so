package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import com.benhsoan.adapter.inbound.rest.mapper.InventoryReceiptRestMapper;
import com.benhsoan.port.dto.result.InventoryReceiptItemResult;
import com.benhsoan.port.dto.result.InventoryReceiptResult;
import com.benhsoan.port.dto.result.InventoryReceiptWarningResult;
import com.benhsoan.port.inbound.inventory.ReceiveStockUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = InventoryReceiptController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(InventoryReceiptRestMapper.class)
@DisplayName("InventoryReceiptController - MockMvc Tests")
class InventoryReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReceiveStockUseCase receiveStockUseCase;

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
    @DisplayName("POST /inventory/receipts returns merged-batch warning metadata")
    void receiveStockReturnsWarnings() throws Exception {
        UUID receiptId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-11T06:00:00Z");

        when(receiveStockUseCase.receiveStock(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new InventoryReceiptResult(
                        receiptId,
                        userId,
                        now,
                        "Nhap them lo cu",
                        now,
                        List.of(new InventoryReceiptItemResult(
                                itemId,
                                medicineId,
                                "BATCH-001",
                                LocalDate.of(2027, 12, 31),
                                30,
                                BigDecimal.valueOf(5000),
                                BigDecimal.valueOf(150000)
                        )),
                        List.of(new InventoryReceiptWarningResult(
                                "MERGED_WITH_EXISTING_BATCH",
                                medicineId,
                                "BATCH-001",
                                "Stock was merged into an existing batch with the same batch number and expiry date."
                        ))
                ));

        mockMvc.perform(post("/inventory/receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Nhap them lo cu",
                                  "items": [
                                    {
                                      "medicineId": "%s",
                                      "batchNumber": "BATCH-001",
                                      "expiryDate": "2027-12-31",
                                      "quantity": 30,
                                      "importPrice": 5000
                                    }
                                  ]
                                }
                                """.formatted(medicineId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(receiptId.toString()))
                .andExpect(jsonPath("$.warnings[0].code").value("MERGED_WITH_EXISTING_BATCH"))
                .andExpect(jsonPath("$.warnings[0].medicineId").value(medicineId.toString()))
                .andExpect(jsonPath("$.warnings[0].batchNumber").value("BATCH-001"));
    }
}
