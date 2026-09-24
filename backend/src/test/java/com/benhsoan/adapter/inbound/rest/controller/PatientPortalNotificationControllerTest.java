package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalNotificationRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.result.portal.PatientPortalNotificationResult;
import com.benhsoan.port.inbound.portal.GetPatientPortalNotificationDetailUseCase;
import com.benhsoan.port.inbound.portal.GetPatientPortalNotificationsUseCase;
import com.benhsoan.port.inbound.portal.MarkPatientPortalNotificationReadUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientPortalNotificationController.class)
@Import({
        PatientPortalNotificationRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class PatientPortalNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GetPatientPortalNotificationsUseCase getNotificationsUseCase;
    @MockitoBean private GetPatientPortalNotificationDetailUseCase getDetailUseCase;
    @MockitoBean private MarkPatientPortalNotificationReadUseCase markReadUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void listReturnsOwnNotifications() throws Exception {
        when(getNotificationsUseCase.getNotifications(50))
                .thenReturn(List.of(result()));

        mockMvc.perform(get("/patient-portal/notifications")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("APPOINTMENT_REMINDER"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void getByIdReturnsNotification() throws Exception {
        UUID id = UUID.randomUUID();
        when(getDetailUseCase.getNotification(id)).thenReturn(result());

        mockMvc.perform(get("/patient-portal/notifications/{id}", id)
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("APPOINTMENT_REMINDER"));
    }

    @Test
    void markReadReturnsNotification() throws Exception {
        UUID id = UUID.randomUUID();
        when(markReadUseCase.markRead(id)).thenReturn(readResult());

        mockMvc.perform(patch("/patient-portal/notifications/{id}/read", id)
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/patient-portal/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonPatientRoleIsRejected() throws Exception {
        mockMvc.perform(get("/patient-portal/notifications")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    private PatientPortalNotificationResult result() {
        return new PatientPortalNotificationResult(
                UUID.randomUUID(), PatientPortalNotificationType.APPOINTMENT_REMINDER,
                "Nhắc lịch hẹn", "message", false, null, Instant.parse("2026-09-30T01:00:00Z"));
    }

    private PatientPortalNotificationResult readResult() {
        return new PatientPortalNotificationResult(
                UUID.randomUUID(), PatientPortalNotificationType.APPOINTMENT_REMINDER,
                "Nhắc lịch hẹn", "message", true, Instant.parse("2026-09-30T02:00:00Z"),
                Instant.parse("2026-09-30T01:00:00Z"));
    }
}
