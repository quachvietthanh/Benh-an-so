package com.benhsoan.adapter.inbound.rest.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.CashierShiftRestMapper;
import com.benhsoan.domain.billing.enums.CashierShiftStatus;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.port.dto.result.CashierShiftResult;
import com.benhsoan.port.dto.result.CurrentShiftSummaryResult;
import com.benhsoan.port.inbound.billing.CloseCashierShiftUseCase;
import com.benhsoan.port.inbound.billing.ConfirmCashierShiftUseCase;
import com.benhsoan.port.inbound.billing.GetCashierShiftByIdUseCase;
import com.benhsoan.port.inbound.billing.GetCurrentShiftSummaryUseCase;
import com.benhsoan.port.inbound.billing.SearchCashierShiftsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CashierShiftController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CashierShiftRestMapper.class, AnonymizationModeState.class})
@DisplayName("CashierShiftController Unit Tests")
class CashierShiftControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetCurrentShiftSummaryUseCase getCurrentShiftSummaryUseCase;
    @MockitoBean private CloseCashierShiftUseCase closeCashierShiftUseCase;
    @MockitoBean private ConfirmCashierShiftUseCase confirmCashierShiftUseCase;
    @MockitoBean private SearchCashierShiftsUseCase searchCashierShiftsUseCase;
    @MockitoBean private GetCashierShiftByIdUseCase getCashierShiftByIdUseCase;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private ClockPort clockPort;

    private final UUID cashierId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-21T09:00:00Z");

    @Test
    @DisplayName("GET /cashier-shifts/current-summary trả về 200 OK kèm dữ liệu tóm tắt ca")
    void getsCurrentShiftSummary() throws Exception {
        when(getCurrentShiftSummaryUseCase.getCurrentSummary()).thenReturn(new CurrentShiftSummaryResult(
                cashierId,
                "Lễ Tân 1",
                Instant.parse("2026-09-21T01:00:00Z"),
                now,
                3,
                new BigDecimal("500000.00"),
                new BigDecimal("300000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("800000.00"),
                List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        ));

        mockMvc.perform(get("/cashier-shifts/current-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashierId").value(cashierId.toString()))
                .andExpect(jsonPath("$.cashierName").value("Lễ Tân 1"))
                .andExpect(jsonPath("$.totalTransactions").value(3))
                .andExpect(jsonPath("$.systemCashAmount").value(500000.00))
                .andExpect(jsonPath("$.totalSystemAmount").value(800000.00));
    }

    @Test
    @DisplayName("POST /cashier-shifts/close trả về 201 Created khi chốt ca thành công")
    void closesShiftSuccessfully() throws Exception {
        UUID shiftId = UUID.randomUUID();
        when(closeCashierShiftUseCase.close(any())).thenReturn(new CashierShiftResult(
                shiftId,
                "CS000001",
                cashierId,
                "Lễ Tân 1",
                Instant.parse("2026-09-21T01:00:00Z"),
                now,
                2,
                new BigDecimal("500000.00"),
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                CashierShiftStatus.CONFIRMED,
                null,
                null,
                null,
                null,
                null,
                now
        ));

        mockMvc.perform(post("/cashier-shifts/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actualCashAmount": 500000.00,
                                  "notes": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(shiftId.toString()))
                .andExpect(jsonPath("$.shiftCode").value("CS000001"))
                .andExpect(jsonPath("$.differenceAmount").value(0))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /cashier-shifts/{id}/confirm trả về 200 OK khi quản lý xác nhận")
    void confirmsShiftSuccessfully() throws Exception {
        UUID shiftId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        when(confirmCashierShiftUseCase.confirm(any())).thenReturn(new CashierShiftResult(
                shiftId,
                "CS000002",
                cashierId,
                "Lễ Tân 1",
                Instant.parse("2026-09-21T01:00:00Z"),
                now,
                1,
                new BigDecimal("500000.00"),
                new BigDecimal("500000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("480000.00"),
                new BigDecimal("-20000.00"),
                CashierShiftStatus.CONFIRMED,
                "Lệch 20k",
                managerId,
                "Quản Lý 1",
                now,
                "Đã đối soát và duyệt",
                now
        ));

        mockMvc.perform(post("/cashier-shifts/{id}/confirm", shiftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmationNotes": "Đã đối soát và duyệt"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shiftId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedBy").value(managerId.toString()))
                .andExpect(jsonPath("$.confirmationNotes").value("Đã đối soát và duyệt"));
    }

    @Test
    @DisplayName("GET /cashier-shifts/{id} trả về 200 OK chi tiết phiếu")
    void getsShiftById() throws Exception {
        UUID shiftId = UUID.randomUUID();
        when(getCashierShiftByIdUseCase.getById(shiftId)).thenReturn(new CashierShiftResult(
                shiftId,
                "CS000001",
                cashierId,
                "Lễ Tân 1",
                Instant.parse("2026-09-21T01:00:00Z"),
                now,
                1,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                CashierShiftStatus.CONFIRMED,
                null,
                null,
                null,
                null,
                null,
                now
        ));

        mockMvc.perform(get("/cashier-shifts/{id}", shiftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shiftId.toString()))
                .andExpect(jsonPath("$.shiftCode").value("CS000001"));
    }

    @Test
    @DisplayName("GET /cashier-shifts trả về danh sách phân trang")
    void searchesShifts() throws Exception {
        UUID shiftId = UUID.randomUUID();
        CashierShiftResult item = new CashierShiftResult(
                shiftId,
                "CS000001",
                cashierId,
                "Lễ Tân 1",
                Instant.parse("2026-09-21T01:00:00Z"),
                now,
                1,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                CashierShiftStatus.CONFIRMED,
                null,
                null,
                null,
                null,
                null,
                now
        );

        when(searchCashierShiftsUseCase.search(any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/cashier-shifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(shiftId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
