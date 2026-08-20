package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import com.benhsoan.adapter.inbound.rest.mapper.PostCareLogRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.PostCareLogResult;
import com.benhsoan.port.inbound.carelog.CreatePostCareLogUseCase;
import com.benhsoan.port.inbound.carelog.GetPatientCareLogsUseCase;
import com.benhsoan.port.inbound.carelog.SearchPostCareLogsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PostCareLogController.class)
@Import({PostCareLogRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class,
        RequirePermissionAspect.class, PermissionEvaluator.class, PostCareLogSecurityIntegrationTest.AspectTestConfig.class})
class PostCareLogSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig { }

    private static final UUID CARE_LOG_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePostCareLogUseCase createPostCareLogUseCase;
    @MockitoBean
    private GetPatientCareLogsUseCase getPatientCareLogsUseCase;
    @MockitoBean
    private SearchPostCareLogsUseCase searchPostCareLogsUseCase;

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
    void allowsAdminReceptionistAndDoctor() throws Exception {
        when(createPostCareLogUseCase.create(any())).thenReturn(result());
        when(getPatientCareLogsUseCase.getForPatient(PATIENT_ID)).thenReturn(List.of(result()));
        when(searchPostCareLogsUseCase.search(any()))
                .thenReturn(new PageImpl<>(List.of(result()), PageRequest.of(0, 20), 1));

        for (String role : new String[]{"ADMIN", "RECEPTIONIST", "DOCTOR"}) {
            mockMvc.perform(post("/care-logs")
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_CARE_LOG_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body()))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/care-logs/patient/{patientId}", PATIENT_ID)
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_CARE_LOG_READ"))))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/care-logs").with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_CARE_LOG_READ"))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void forbidsUnauthorizedRoles() throws Exception {
        for (String role : new String[]{"NURSE", "PHARMACIST", "MANAGER"}) {
            mockMvc.perform(post("/care-logs")
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_CARE_LOG_READ")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body()))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/care-logs/patient/{patientId}", PATIENT_ID)
                            .with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_CARE_LOG_CREATE"))))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/care-logs").with(user(role.toLowerCase()).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_CARE_LOG_CREATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    private String body() {
        return """
                {
                  "patientId": "%s",
                  "contactChannel": "PHONE",
                  "contactedAt": "2026-08-15T08:00:00Z",
                  "patientCondition": "STABLE",
                  "careNotes": "Benh nhan on dinh",
                  "contactOutcome": "REACHED"
                }
                """.formatted(PATIENT_ID);
    }

    private PostCareLogResult result() {
        return new PostCareLogResult(
                CARE_LOG_ID,
                PATIENT_ID,
                null,
                null,
                ContactChannel.PHONE,
                Instant.parse("2026-08-15T08:00:00Z"),
                PatientCondition.STABLE,
                "Benh nhan on dinh",
                ContactOutcome.REACHED,
                UUID.randomUUID(),
                Instant.parse("2026-08-15T08:00:00Z")
        );
    }
}
