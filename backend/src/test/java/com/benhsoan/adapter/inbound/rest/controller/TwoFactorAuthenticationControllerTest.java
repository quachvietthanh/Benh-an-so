package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.benhsoan.adapter.inbound.rest.mapper.AuthRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.domain.auth.exception.InvalidVerificationCodeException;
import com.benhsoan.domain.auth.exception.TwoFactorChallengeInvalidException;
import com.benhsoan.domain.auth.exception.VerificationCodeExpiredException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.dto.result.TwoFactorConfigurationResult;
import com.benhsoan.port.dto.result.TwoFactorResendResult;
import com.benhsoan.port.inbound.auth.TwoFactorAuthenticationConfigurationUseCase;
import com.benhsoan.port.inbound.auth.TwoFactorResendUseCase;
import com.benhsoan.port.inbound.auth.TwoFactorVerificationUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@WebMvcTest(TwoFactorAuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AuthRestMapper.class, AnonymizationModeState.class,
        RequirePermissionAspect.class, PermissionEvaluator.class, TwoFactorAuthenticationControllerTest.AspectConfiguration.class})
class TwoFactorAuthenticationControllerTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectConfiguration {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TwoFactorVerificationUseCase twoFactorVerificationUseCase;
    @MockitoBean
    private TwoFactorResendUseCase twoFactorResendUseCase;
    @MockitoBean
    private TwoFactorAuthenticationConfigurationUseCase configurationUseCase;
    @MockitoBean
    private AuditLogRepository auditLogRepository;
    @MockitoBean
    private CurrentUserPort currentUserPort;
    @MockitoBean
    private com.benhsoan.port.outbound.authSecurity.JwtTokenPort jwtTokenPort;
    @MockitoBean
    private com.benhsoan.port.outbound.repository.auth.UserRepository userRepository;
    @MockitoBean
    private com.benhsoan.port.outbound.repository.auth.UserSessionRepository userSessionRepository;
    @MockitoBean
    private com.benhsoan.port.outbound.time.ClockPort clockPort;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void verifySecondFactor_successReturnsTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        when(twoFactorVerificationUseCase.verify(any()))
                .thenReturn(new LoginResult(userId, "doctor1", "access-token", "refresh-token", "DOCTOR",
                        Instant.parse("2026-09-22T11:00:00Z")));

        mvc.perform(post("/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"twoFactorToken\":\"" + UUID.randomUUID() + "\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.twoFactorRequired").value(false));
    }

    @Test
    void verifySecondFactor_capturesXForwardedForIp() throws Exception {
        UUID userId = UUID.randomUUID();
        when(twoFactorVerificationUseCase.verify(any()))
                .thenReturn(new LoginResult(userId, "doctor1", "access-token", "refresh-token", "DOCTOR",
                        Instant.parse("2026-09-22T11:00:00Z")));

        org.mockito.ArgumentCaptor<com.benhsoan.port.dto.command.auth.VerifyTwoFactorCommand> captor =
                org.mockito.ArgumentCaptor.forClass(com.benhsoan.port.dto.command.auth.VerifyTwoFactorCommand.class);

        mvc.perform(post("/auth/2fa/verify")
                        .header("X-Forwarded-For", "198.51.100.4, 10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"twoFactorToken\":\"" + UUID.randomUUID() + "\",\"code\":\"123456\"}"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(twoFactorVerificationUseCase).verify(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("198.51.100.4", captor.getValue().ipAddress());
    }

    @Test
    void verifySecondFactor_invalidCodeReturns400() throws Exception {
        when(twoFactorVerificationUseCase.verify(any())).thenThrow(new InvalidVerificationCodeException());

        mvc.perform(post("/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"twoFactorToken\":\"" + UUID.randomUUID() + "\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    void verifySecondFactor_expiredCodeReturns400() throws Exception {
        when(twoFactorVerificationUseCase.verify(any())).thenThrow(new VerificationCodeExpiredException());

        mvc.perform(post("/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"twoFactorToken\":\"" + UUID.randomUUID() + "\",\"code\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_EXPIRED"));
    }

    @Test
    void verifySecondFactor_invalidChallengeReturns401() throws Exception {
        when(twoFactorVerificationUseCase.verify(any())).thenThrow(new TwoFactorChallengeInvalidException());

        mvc.perform(post("/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"twoFactorToken\":\"" + UUID.randomUUID() + "\",\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TWO_FACTOR_CHALLENGE_INVALID"));
    }

    @Test
    void resendSecondFactor_returnsNewExpiry() throws Exception {
        UUID token = UUID.randomUUID();
        when(twoFactorResendUseCase.resend(any()))
                .thenReturn(new TwoFactorResendResult(token, Instant.parse("2026-09-22T11:00:00Z")));

        mvc.perform(post("/auth/2fa/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"twoFactorToken\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twoFactorToken").value(token.toString()));
    }

    @Test
    void configureTwoFactor_withPermissionReturns200() throws Exception {
        when(configurationUseCase.configure(any()))
                .thenReturn(new TwoFactorConfigurationResult("DOCTOR", true));

        mvc.perform(patch("/admin/two-factor-auth/roles/DOCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}")
                        .with(withPermission("PERMISSION_TWO_FACTOR_AUTH_MANAGE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twoFactorRequired").value(true));
    }

    @Test
    void configureTwoFactor_withoutPermissionReturns403() throws Exception {
        mvc.perform(patch("/admin/two-factor-auth/roles/DOCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}")
                        .with(withPermission("PERMISSION_USER_READ")))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor withPermission(String permission) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "user", null, List.of(new SimpleGrantedAuthority(permission))));
            return request;
        };
    }
}
