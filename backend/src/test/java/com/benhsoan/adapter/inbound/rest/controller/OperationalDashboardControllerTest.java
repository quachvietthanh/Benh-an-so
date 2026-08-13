package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.port.dto.result.OperationalDashboardResult;
import com.benhsoan.port.inbound.dashboard.GetOperationalDashboardUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = OperationalDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OperationalDashboardController - MockMvc Tests")
class OperationalDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetOperationalDashboardUseCase getOperationalDashboardUseCase;

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
    @DisplayName("GET /dashboard/operational returns today's operational metrics")
    void returnsOperationalDashboard() throws Exception {
        when(getOperationalDashboardUseCase.get())
                .thenReturn(new OperationalDashboardResult(
                        new OperationalDashboardResult.VisitSummary(10, 3, 2, 4, 1),
                        new OperationalDashboardResult.RevenueSummary(new BigDecimal("999.00")),
                        new OperationalDashboardResult.InventoryAlertSummary(2, 3),
                        Instant.parse("2026-08-11T08:00:00Z")
                ));

        mockMvc.perform(get("/dashboard/operational"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitSummary.total").value(10))
                .andExpect(jsonPath("$.visitSummary.waiting").value(3))
                .andExpect(jsonPath("$.visitSummary.inProgress").value(2))
                .andExpect(jsonPath("$.visitSummary.completed").value(4))
                .andExpect(jsonPath("$.visitSummary.cancelled").value(1))
                .andExpect(jsonPath("$.revenueSummary.totalRevenueToday").value(999.00))
                .andExpect(jsonPath("$.inventoryAlertSummary.lowStockCount").value(2))
                .andExpect(jsonPath("$.inventoryAlertSummary.expiryAlertCount").value(3))
                .andExpect(jsonPath("$.asOf").value("2026-08-11T08:00:00Z"));
    }

    @Test
    @DisplayName("GET /dashboard/operational returns zeroes when day has no data")
    void returnsZeroesForEmptyDay() throws Exception {
        when(getOperationalDashboardUseCase.get())
                .thenReturn(new OperationalDashboardResult(
                        new OperationalDashboardResult.VisitSummary(0, 0, 0, 0, 0),
                        new OperationalDashboardResult.RevenueSummary(BigDecimal.ZERO),
                        new OperationalDashboardResult.InventoryAlertSummary(0, 0),
                        Instant.parse("2026-08-11T08:00:00Z")
                ));

        mockMvc.perform(get("/dashboard/operational"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitSummary.total").value(0))
                .andExpect(jsonPath("$.visitSummary.waiting").value(0))
                .andExpect(jsonPath("$.visitSummary.inProgress").value(0))
                .andExpect(jsonPath("$.visitSummary.completed").value(0))
                .andExpect(jsonPath("$.visitSummary.cancelled").value(0))
                .andExpect(jsonPath("$.revenueSummary.totalRevenueToday").value(0))
                .andExpect(jsonPath("$.inventoryAlertSummary.lowStockCount").value(0))
                .andExpect(jsonPath("$.inventoryAlertSummary.expiryAlertCount").value(0));
    }
}
