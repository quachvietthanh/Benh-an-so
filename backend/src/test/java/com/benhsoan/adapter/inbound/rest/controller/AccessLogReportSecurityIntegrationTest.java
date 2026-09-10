package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.reporting.exception.AccessLogReportDataEmptyException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.AccessLogReportExportResult;
import com.benhsoan.port.inbound.reporting.ExportAccessLogReportUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = AccessLogReportController.class)
@Import({
        AopAutoConfiguration.class,
        AccessLogReportSecurityIntegrationTest.AspectTestConfig.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class AccessLogReportSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportAccessLogReportUseCase exportAccessLogReportUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private ClockPort clockPort;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @Test
    void forbidsUserWithoutAccessLogReportExportPermission() throws Exception {
        mockMvc.perform(get("/reports/access-log/export")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30")
                        .with(user("manager").authorities(
                                new SimpleGrantedAuthority("PERMISSION_AUDIT_READ"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(exportAccessLogReportUseCase);
    }

    @Test
    void allowsAdminWithAccessLogReportExportPermissionAndReturnsCsv() throws Exception {
        byte[] csv = "Account,Full Name,Access Count\nadmin,System Administrator,5\n"
                .getBytes(StandardCharsets.UTF_8);
        when(exportAccessLogReportUseCase.export(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new AccessLogReportExportResult(
                        "access-log-report-2026-09-01-to-2026-09-30.csv",
                        "text/csv; charset=UTF-8",
                        csv));

        mockMvc.perform(get("/reports/access-log/export")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30")
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("PERMISSION_ACCESS_LOG_REPORT_EXPORT"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"access-log-report-2026-09-01-to-2026-09-30.csv\""))
                .andExpect(MockMvcResultMatchers.content().bytes(csv));
    }

    @Test
    void returnsUnprocessableEntityWhenNoAccessLogsInPeriod() throws Exception {
        when(exportAccessLogReportUseCase.export(any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new AccessLogReportDataEmptyException());

        mockMvc.perform(get("/reports/access-log/export")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30")
                        .with(admin()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("REPORT_DATA_EMPTY"))
                .andExpect(jsonPath("$.message")
                        .value("No medical record access logs available for the selected period."))
                .andExpect(header().doesNotExist("Content-Disposition"));
    }

    @Test
    void rejectsInvalidDateRangeBeforeCallingUseCase() throws Exception {
        mockMvc.perform(get("/reports/access-log/export")
                        .param("from", "2026-09-30")
                        .param("to", "2026-09-01")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be before or equal to to."));

        verifyNoInteractions(exportAccessLogReportUseCase);
    }

    @Test
    void rejectsMissingFromParameter() throws Exception {
        mockMvc.perform(get("/reports/access-log/export")
                        .param("to", "2026-09-30")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("from is required."));
    }

    @Test
    void rejectsMissingToParameter() throws Exception {
        mockMvc.perform(get("/reports/access-log/export")
                        .param("from", "2026-09-01")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("to is required."));
    }

    @Test
    void rejectsInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/reports/access-log/export")
                        .param("from", "2026/09/01")
                        .param("to", "2026-09-30")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be in yyyy-MM-dd format."));
    }

    @Test
    void rejectsRangeExceeding366Days() throws Exception {
        mockMvc.perform(get("/reports/access-log/export")
                        .param("from", "2025-01-01")
                        .param("to", "2026-01-02")
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date range must not exceed 366 days."));
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/reports/access-log/export")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(exportAccessLogReportUseCase);
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("admin").authorities(
                new SimpleGrantedAuthority("PERMISSION_ACCESS_LOG_REPORT_EXPORT"));
    }
}
