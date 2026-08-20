package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.FollowUpReminderRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.FollowUpReminderResult;
import com.benhsoan.port.inbound.followup.CreateFollowUpReminderUseCase;
import com.benhsoan.port.inbound.followup.GetDueFollowUpRemindersUseCase;
import com.benhsoan.port.inbound.followup.SearchFollowUpRemindersUseCase;
import com.benhsoan.port.inbound.followup.UpdateFollowUpReminderStatusUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = FollowUpReminderController.class)
@Import({FollowUpReminderRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class,
        RequirePermissionAspect.class, PermissionEvaluator.class, FollowUpReminderSecurityIntegrationTest.AspectTestConfig.class})
class FollowUpReminderSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig { }

    private static final UUID REMINDER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateFollowUpReminderUseCase createFollowUpReminderUseCase;
    @MockitoBean
    private GetDueFollowUpRemindersUseCase getDueFollowUpRemindersUseCase;
    @MockitoBean
    private SearchFollowUpRemindersUseCase searchFollowUpRemindersUseCase;
    @MockitoBean
    private UpdateFollowUpReminderStatusUseCase updateFollowUpReminderStatusUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserSessionRepository userSessionRepository;
    @MockitoBean
    private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void allowsAdminAndReceptionist() throws Exception {
        when(createFollowUpReminderUseCase.create(any())).thenReturn(result());
        when(getDueFollowUpRemindersUseCase.getDue(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(result()), PageRequest.of(0, 20), 1));
        when(searchFollowUpRemindersUseCase.search(any()))
                .thenReturn(new PageImpl<>(List.of(result()), PageRequest.of(0, 20), 1));
        when(updateFollowUpReminderStatusUseCase.updateStatus(any(), any())).thenReturn(result());

        for (String role : new String[]{"ADMIN", "RECEPTIONIST"}) {
            mockMvc.perform(post("/follow-up-reminders")
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_FOLLOW_UP_REMINDER_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"patientId\":\"" + REMINDER_ID + "\",\"visitId\":\"" + REMINDER_ID + "\",\"followUpDate\":\"2026-08-30\",\"remindAt\":\"2026-08-15T08:00:00Z\"}"))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/follow-up-reminders/due").with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_FOLLOW_UP_REMINDER_READ"))))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/follow-up-reminders").with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_FOLLOW_UP_REMINDER_READ"))))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/follow-up-reminders/{id}/status", REMINDER_ID)
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_FOLLOW_UP_REMINDER_UPDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"SENT\"}"))
                    .andExpect(status().isOk());
        }
    }
    @Test
    void forbidsUnauthorizedRoles() throws Exception {
        for (String role : new String[]{"DOCTOR", "NURSE", "PHARMACIST", "MANAGER"}) {
            mockMvc.perform(post("/follow-up-reminders")
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_FOLLOW_UP_REMINDER_READ")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"patientId\":\"" + REMINDER_ID + "\",\"visitId\":\"" + REMINDER_ID + "\",\"followUpDate\":\"2026-08-30\",\"remindAt\":\"2026-08-15T08:00:00Z\"}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/follow-up-reminders/due").with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_FOLLOW_UP_REMINDER_CREATE"))))
                    .andExpect(status().isForbidden());

            mockMvc.perform(patch("/follow-up-reminders/{id}/status", REMINDER_ID)
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_FOLLOW_UP_REMINDER_READ")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"SENT\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    private FollowUpReminderResult result() {
        return new FollowUpReminderResult(
                REMINDER_ID,
                UUID.randomUUID(),
                null,
                null,
                LocalDate.of(2026, 8, 30),
                Instant.parse("2026-08-15T08:00:00Z"),
                ReminderType.GENERAL,
                ReminderStatus.PENDING,
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-15T08:00:00Z")
        );
    }
}
