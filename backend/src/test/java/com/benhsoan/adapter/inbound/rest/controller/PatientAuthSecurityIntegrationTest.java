package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auth.exception.InvalidCredentialsException;
import com.benhsoan.domain.auth.exception.TooManyLoginAttemptsException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.command.auth.PatientLoginCommand;
import com.benhsoan.port.dto.result.PatientLoginResult;
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

@WebMvcTest(controllers = {AuthController.class, PatientPasswordRecoveryController.class})
@Import({AnonymizationModeState.class, 
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
    @MockitoBean private PatientPortalRegistrationUseCase patientPortalRegistrationUseCase;
    @MockitoBean private ChangePasswordUseCase changePasswordUseCase;
    @MockitoBean private com.benhsoan.port.inbound.auth.PatientForgotPasswordUseCase patientForgotPasswordUseCase;
    @MockitoBean private com.benhsoan.port.inbound.auth.PatientVerifyRecoveryCodeUseCase patientVerifyRecoveryCodeUseCase;
    @MockitoBean private com.benhsoan.port.inbound.auth.PatientResetPasswordUseCase patientResetPasswordUseCase;
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
    void patientLoginLockedReturns429WithRetryAfter() throws Exception {
        when(patientLoginUseCase.login(any(PatientLoginCommand.class)))
                .thenThrow(new TooManyLoginAttemptsException(45, Instant.now().plusSeconds(45)));

        mockMvc.perform(post("/auth/patient/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901111222\",\"password\":\"secret\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "45"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_LOGIN_ATTEMPTS"))
                .andExpect(jsonPath("$.details.retryAfterSeconds").value(45));
    }

    @Test
    void patientForgotPassword_publicEndpointReturns200WithGenericMessage() throws Exception {
        when(patientForgotPasswordUseCase.forgotPassword(any(com.benhsoan.port.dto.command.auth.PatientForgotPasswordCommand.class)))
                .thenReturn(new com.benhsoan.port.dto.result.PatientForgotPasswordResult(
                        "Nếu số điện thoại đã được đăng ký tài khoản bệnh nhân, mã xác thực sẽ được gửi tới số điện thoại của bạn.", 300));

        mockMvc.perform(post("/auth/patient/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901111222\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(300))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void patientVerifyRecoveryCode_validReturns200() throws Exception {
        mockMvc.perform(post("/auth/patient/verify-recovery-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901111222\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void patientVerifyRecoveryCode_expiredReturns400() throws Exception {
        org.mockito.Mockito.doThrow(new com.benhsoan.domain.auth.exception.VerificationCodeExpiredException())
                .when(patientVerifyRecoveryCodeUseCase).verifyCode(any(com.benhsoan.port.dto.command.auth.PatientVerifyRecoveryCodeCommand.class));

        mockMvc.perform(post("/auth/patient/verify-recovery-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901111222\",\"code\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_EXPIRED"));
    }

    @Test
    void patientResetPassword_successReturns200() throws Exception {
        when(patientResetPasswordUseCase.resetPassword(any(com.benhsoan.port.dto.command.auth.PatientResetPasswordCommand.class)))
                .thenReturn(new com.benhsoan.port.dto.result.PatientResetPasswordResult(
                        "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."));

        mockMvc.perform(post("/auth/patient/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901111222\",\"code\":\"123456\",\"newPassword\":\"NewPassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."));
    }

    @Test
    void patientResetPassword_weakPasswordReturns400() throws Exception {
        org.mockito.Mockito.doThrow(new com.benhsoan.domain.auth.exception.WeakPasswordException(
                java.util.List.of("Mật khẩu phải chứa ít nhất một chữ số (0-9).")))
                .when(patientResetPasswordUseCase).resetPassword(any(com.benhsoan.port.dto.command.auth.PatientResetPasswordCommand.class));

        mockMvc.perform(post("/auth/patient/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0901111222\",\"code\":\"123456\",\"newPassword\":\"Weakpassword\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"))
                .andExpect(jsonPath("$.details.violations").isArray());
    }
}
