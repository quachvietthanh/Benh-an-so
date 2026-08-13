package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.ReportingRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.enums.Permission;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.PermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.inbound.reporting.ExportOperationalReportUseCase;
import com.benhsoan.port.inbound.reporting.GetOperationalSummaryUseCase;
import com.benhsoan.port.inbound.reporting.GetOperationalTimelineUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ReportsController.class)
@Import({
        AopAutoConfiguration.class,
        ReportsSecurityIntegrationTest.AspectTestConfig.class,
        ReportingRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        PermissionAspect.class,
        PermissionEvaluator.class
})
class ReportsSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetOperationalSummaryUseCase getOperationalSummaryUseCase;
    @MockitoBean private GetOperationalTimelineUseCase getOperationalTimelineUseCase;
    @MockitoBean private ExportOperationalReportUseCase exportOperationalReportUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;

    @BeforeEach
    void setUp() {
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(
                Role.create("MANAGER", "Clinic manager", true, Set.of(Permission.REPORT_VIEW, Permission.REPORT_EXPORT))
        ));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(
                Role.create("ADMIN", "Admin", true, Set.of(Permission.USER_READ))
        ));
        when(roleRepository.findByName("DOCTOR")).thenReturn(Optional.of(
                Role.create("DOCTOR", "Doctor", true, Set.of(Permission.PATIENT_READ))
        ));
    }

    @Test
    void forbidsDoctorWithoutReportingPermission() throws Exception {
        mockMvc.perform(get("/reports/summary")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getOperationalSummaryUseCase);
    }

    @Test
    void forbidsAdminBecauseReportingBelongsToManagerRole() throws Exception {
        mockMvc.perform(get("/reports/summary")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getOperationalSummaryUseCase);
    }

    @Test
    void allowsManagerWithReportViewPermission() throws Exception {
        when(getOperationalSummaryUseCase.getSummary(any(), any())).thenReturn(new OperationalSummaryResult(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3),
                10L,
                new BigDecimal("2000000"),
                "VND"
        ));

        mockMvc.perform(get("/reports/summary")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void forbidsAdminFromExportingOperationalReport() throws Exception {
        mockMvc.perform(get("/reports/export")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-03")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
