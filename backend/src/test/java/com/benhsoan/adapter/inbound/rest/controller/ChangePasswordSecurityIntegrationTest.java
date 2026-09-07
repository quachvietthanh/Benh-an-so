package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.AuthRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.command.auth.ChangePasswordCommand;
import com.benhsoan.port.inbound.auth.ChangePasswordUseCase;
import com.benhsoan.port.inbound.auth.LoginUseCase;
import com.benhsoan.port.inbound.auth.LogoutUseCase;
import com.benhsoan.port.inbound.auth.PatientLoginUseCase;
import com.benhsoan.port.inbound.auth.PatientPortalRegistrationUseCase;
import com.benhsoan.port.inbound.auth.RefreshTokenUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = AuthController.class)
@Import({
        AuthRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class ChangePasswordSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private LoginUseCase loginUseCase;
    @MockitoBean private LogoutUseCase logoutUseCase;
    @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockitoBean private PatientLoginUseCase patientLoginUseCase;
    @MockitoBean private PatientPortalRegistrationUseCase patientPortalRegistrationUseCase;
    @MockitoBean private ChangePasswordUseCase changePasswordUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-07T12:00:00Z");

    @Test
    @DisplayName("POST /auth/change-password without token returns 401 Unauthorized (Finding 4)")
    void changePasswordWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "oldPassword": "OldPassword123",
                                    "newPassword": "NewPassword123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("POST /auth/change-password with valid token succeeds (Finding 4 / TC-01)")
    void changePasswordWithValidTokenSucceeds() throws Exception {
        mockActiveUser(false);
        doNothing().when(changePasswordUseCase).changePassword(any(ChangePasswordCommand.class));

        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "oldPassword": "OldPassword123",
                                    "newPassword": "NewPassword123"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/change-password allowed when mustChangePassword is true (Finding 2)")
    void changePasswordAllowedWhenMustChangePasswordIsTrue() throws Exception {
        mockActiveUser(true);
        doNothing().when(changePasswordUseCase).changePassword(any(ChangePasswordCommand.class));

        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "oldPassword": "TempPassword123",
                                    "newPassword": "NewSecurePassword123"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Non-auth business API blocked with 403 MUST_CHANGE_PASSWORD when mustChangePassword is true (Finding 2)")
    void businessApiBlockedWhenMustChangePasswordIsTrue() throws Exception {
        mockActiveUser(true);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MUST_CHANGE_PASSWORD"));
    }

    private void mockActiveUser(boolean mustChangePassword) {
        when(jwtTokenPort.validate("valid-token")).thenReturn(true);
        when(jwtTokenPort.getUserId("valid-token")).thenReturn(userId);
        when(jwtTokenPort.getSessionId("valid-token")).thenReturn(sessionId);
        when(jwtTokenPort.getUsername("valid-token")).thenReturn("staff1");
        when(jwtTokenPort.getRole("valid-token")).thenReturn("DOCTOR");
        when(jwtTokenPort.getPermissions("valid-token")).thenReturn(java.util.Set.of("USER_READ"));
        when(clockPort.now()).thenReturn(now);

        UserSession session = UserSession.restore(sessionId, userId, "hash", null,
                now.plus(Duration.ofDays(7)), now, now, null);
        when(userSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        User user = User.restore(userId, "staff1", "$2a$10$hashed", "Staff One",
                "staff1@benhsoan.com", "0900000001", UUID.randomUUID(), true, mustChangePassword, null, now);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }
}
