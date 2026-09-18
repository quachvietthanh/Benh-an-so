package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.AuthRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auth.exception.InvalidVerificationCodeException;
import com.benhsoan.domain.auth.exception.VerificationCodeCooldownException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.command.auth.PatientForgotPasswordCommand;
import com.benhsoan.port.dto.command.auth.PatientResetPasswordCommand;
import com.benhsoan.port.dto.command.auth.PatientVerifyRecoveryCodeCommand;
import com.benhsoan.port.dto.result.PatientForgotPasswordResult;
import com.benhsoan.port.dto.result.PatientResetPasswordResult;
import com.benhsoan.port.inbound.auth.PatientForgotPasswordUseCase;
import com.benhsoan.port.inbound.auth.PatientResetPasswordUseCase;
import com.benhsoan.port.inbound.auth.PatientVerifyRecoveryCodeUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientPasswordRecoveryController.class)
@Import({
        AnonymizationModeState.class,
        AuthRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
@DisplayName("Patient Password Recovery Controller Tests")
class PatientPasswordRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private PatientForgotPasswordUseCase patientForgotPasswordUseCase;
    @MockitoBean private PatientVerifyRecoveryCodeUseCase patientVerifyRecoveryCodeUseCase;
    @MockitoBean private PatientResetPasswordUseCase patientResetPasswordUseCase;

    // Infrastructure mocks needed for SecurityConfig & JwtFilter
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    @DisplayName("POST /auth/patient/forgot-password returns 200 with generic message")
    void forgotPassword_returns200() throws Exception {
        when(patientForgotPasswordUseCase.forgotPassword(any(PatientForgotPasswordCommand.class)))
                .thenReturn(new PatientForgotPasswordResult("Nếu số điện thoại đã được đăng ký, mã sẽ được gửi.", 300));

        mockMvc.perform(post("/auth/patient/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901234567\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(300))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /auth/patient/forgot-password in cooldown returns 429 Too Many Requests")
    void forgotPassword_cooldownReturns429() throws Exception {
        when(patientForgotPasswordUseCase.forgotPassword(any(PatientForgotPasswordCommand.class)))
                .thenThrow(new VerificationCodeCooldownException(45));

        mockMvc.perform(post("/auth/patient/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901234567\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_COOLDOWN"));
    }

    @Test
    @DisplayName("POST /auth/patient/verify-recovery-code returns 200 when valid")
    void verifyRecoveryCode_returns200() throws Exception {
        mockMvc.perform(post("/auth/patient/verify-recovery-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901234567\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("POST /auth/patient/verify-recovery-code returns 400 when invalid")
    void verifyRecoveryCode_invalidReturns400() throws Exception {
        doThrow(new InvalidVerificationCodeException())
                .when(patientVerifyRecoveryCodeUseCase).verifyCode(any(PatientVerifyRecoveryCodeCommand.class));

        mockMvc.perform(post("/auth/patient/verify-recovery-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901234567\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"));
    }

    @Test
    @DisplayName("POST /auth/patient/reset-password returns 200 when successful")
    void resetPassword_returns200() throws Exception {
        when(patientResetPasswordUseCase.resetPassword(any(PatientResetPasswordCommand.class)))
                .thenReturn(new PatientResetPasswordResult("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."));

        mockMvc.perform(post("/auth/patient/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901234567\",\"code\":\"123456\",\"newPassword\":\"NewPass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
