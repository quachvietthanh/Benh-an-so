package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.AuthRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.SessionRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.result.auth.ExtendSessionResult;
import com.benhsoan.port.inbound.auth.ChangePasswordUseCase;
import com.benhsoan.port.inbound.auth.ExtendSessionUseCase;
import com.benhsoan.port.inbound.auth.LoginUseCase;
import com.benhsoan.port.inbound.auth.LogoutUseCase;
import com.benhsoan.port.inbound.auth.PatientLoginUseCase;
import com.benhsoan.port.inbound.auth.PatientPortalRegistrationUseCase;
import com.benhsoan.port.inbound.auth.RefreshTokenUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = AuthController.class)
@Import({
        AnonymizationModeState.class,
        AuthRestMapper.class,
        SessionRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
@DisplayName("AuthController Security & Extend Session Tests (AC-02, Finding-03)")
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private LoginUseCase loginUseCase;
    @MockitoBean private LogoutUseCase logoutUseCase;
    @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockitoBean private PatientLoginUseCase patientLoginUseCase;
    @MockitoBean private PatientPortalRegistrationUseCase patientPortalRegistrationUseCase;
    @MockitoBean private ChangePasswordUseCase changePasswordUseCase;
    @MockitoBean private ExtendSessionUseCase extendSessionUseCase;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Gia hạn phiên khi chưa đăng nhập bị từ chối 401 Unauthorized")
    void extendSessionUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(post("/auth/sessions/current/extend"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC-02: Gia hạn phiên khi đã đăng nhập thành công trả về 200 OK")
    void extendSessionAuthenticatedSuccess() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant idleExpiresAt = now.plusSeconds(1800);

        when(extendSessionUseCase.extendCurrentSession()).thenReturn(
                new ExtendSessionResult(sessionId, now, idleExpiresAt, "Session extended successfully")
        );

        mockMvc.perform(post("/auth/sessions/current/extend")
                        .with(user("doctor1").authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.message").value("Session extended successfully"));
    }

    @Test
    @DisplayName("Finding-03: Gia hạn phiên thất bại trả về 400 Bad Request VALIDATION_FAILED thay vì 500")
    void extendSessionExpiredReturnsBadRequest() throws Exception {
        when(extendSessionUseCase.extendCurrentSession()).thenThrow(
                new ValidationException("Cannot extend an idle timed-out session.")
        );

        mockMvc.perform(post("/auth/sessions/current/extend")
                        .with(user("doctor1").authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Cannot extend an idle timed-out session."));
    }
}
