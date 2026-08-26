package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.AuthRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auth.exception.InvalidCredentialsException;
import com.benhsoan.domain.auth.exception.TooManyLoginAttemptsException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.command.auth.PatientLoginCommand;
import com.benhsoan.port.dto.result.PatientLoginResult;
import com.benhsoan.port.inbound.auth.LoginUseCase;
import com.benhsoan.port.inbound.auth.LogoutUseCase;
import com.benhsoan.port.inbound.auth.PatientLoginUseCase;
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
class PatientAuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private LoginUseCase loginUseCase;
    @MockitoBean private LogoutUseCase logoutUseCase;
    @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockitoBean private PatientLoginUseCase patientLoginUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void patientLoginSuccessReturns200WithRoleAndPatientId() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(patientLoginUseCase.login(any(PatientLoginCommand.class)))
                .thenReturn(new PatientLoginResult(
                        UUID.randomUUID(), "patient1", "access", "refresh",
                        "PATIENT", Instant.now().plusSeconds(900), patientId));

        mockMvc.perform(post("/auth/patient/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901111222\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()));
    }

    @Test
    void patientLoginInvalidCredentialsReturns401() throws Exception {
        when(patientLoginUseCase.login(any(PatientLoginCommand.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/patient/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901111222\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patientLoginLockedReturns429() throws Exception {
        when(patientLoginUseCase.login(any(PatientLoginCommand.class)))
                .thenThrow(new TooManyLoginAttemptsException());

        mockMvc.perform(post("/auth/patient/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901111222\",\"password\":\"secret\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
