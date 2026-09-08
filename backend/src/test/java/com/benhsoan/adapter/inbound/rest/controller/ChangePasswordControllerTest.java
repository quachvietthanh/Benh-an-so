package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.AuthRestMapper;
import com.benhsoan.domain.auth.exception.InvalidOldPasswordException;
import com.benhsoan.domain.auth.exception.WeakPasswordException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.port.dto.command.auth.ChangePasswordCommand;
import com.benhsoan.port.inbound.auth.ChangePasswordUseCase;
import com.benhsoan.port.inbound.auth.LoginUseCase;
import com.benhsoan.port.inbound.auth.LogoutUseCase;
import com.benhsoan.port.inbound.auth.PatientLoginUseCase;
import com.benhsoan.port.inbound.auth.PatientPortalRegistrationUseCase;
import com.benhsoan.port.inbound.auth.RefreshTokenUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        AuthRestMapper.class,
        GlobalExceptionHandler.class
})
class ChangePasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private LoginUseCase loginUseCase;
    @MockitoBean private LogoutUseCase logoutUseCase;
    @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockitoBean private PatientLoginUseCase patientLoginUseCase;
    @MockitoBean private PatientPortalRegistrationUseCase patientPortalRegistrationUseCase;
    @MockitoBean private ChangePasswordUseCase changePasswordUseCase;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;

    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("POST /auth/change-password succeeds with valid input (TC-01)")
    void changePasswordSuccess() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        doNothing().when(changePasswordUseCase).changePassword(any(ChangePasswordCommand.class));

        mockMvc.perform(post("/auth/change-password")
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
    @DisplayName("POST /auth/change-password returns 400 when old password is blank")
    void changePasswordMissingOldPasswordFails() throws Exception {
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "oldPassword": "",
                                    "newPassword": "NewPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /auth/change-password returns 400 with details when new password is weak (TC-02)")
    void changePasswordWeakPasswordFails() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        doThrow(new WeakPasswordException(List.of("Mật khẩu phải có độ dài từ 8 đến 50 ký tự.")))
                .when(changePasswordUseCase).changePassword(any(ChangePasswordCommand.class));

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "oldPassword": "OldPassword123",
                                    "newPassword": "weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"))
                .andExpect(jsonPath("$.details.violations").isArray());
    }

    @Test
    @DisplayName("POST /auth/change-password returns 400 when old password is incorrect")
    void changePasswordIncorrectOldPasswordFails() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        doThrow(new InvalidOldPasswordException())
                .when(changePasswordUseCase).changePassword(any(ChangePasswordCommand.class));

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "oldPassword": "WrongOldPassword",
                                    "newPassword": "ValidNewPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OLD_PASSWORD"));
    }

    @Test
    @DisplayName("POST /auth/change-password returns 400 when new password is same as old password (Finding 6)")
    void changePasswordSamePasswordFails() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        doThrow(new com.benhsoan.domain.auth.exception.SamePasswordException())
                .when(changePasswordUseCase).changePassword(any(ChangePasswordCommand.class));

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "oldPassword": "OldPassword123",
                                    "newPassword": "OldPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SAME_PASSWORD_NOT_ALLOWED"));
    }
}
