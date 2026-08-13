package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.config.SecurityConfig;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.result.OperationalDashboardResult;
import com.benhsoan.port.inbound.dashboard.GetOperationalDashboardUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = OperationalDashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class OperationalDashboardSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetOperationalDashboardUseCase getOperationalDashboardUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private ClockPort clockPort;

    @Test
    void onlyAdminsAndClinicManagersCanReadOperationalDashboard() throws Exception {
        when(getOperationalDashboardUseCase.get())
                .thenReturn(new OperationalDashboardResult(
                        new OperationalDashboardResult.VisitSummary(0, 0, 0, 0, 0),
                        new OperationalDashboardResult.RevenueSummary(BigDecimal.ZERO),
                        new OperationalDashboardResult.InventoryAlertSummary(0, 0),
                        Instant.parse("2026-08-11T08:00:00Z")
                ));

        mockMvc.perform(get("/dashboard/operational").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dashboard/operational").with(user("manager").roles("CLINIC_MANAGER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard/operational").with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/dashboard/operational").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }
}
