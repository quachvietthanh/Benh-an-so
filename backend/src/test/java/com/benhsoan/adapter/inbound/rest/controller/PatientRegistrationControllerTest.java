package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.benhsoan.domain.auth.exception.PhoneAlreadyExistsException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.command.auth.PatientPortalRegistrationCommand;
import com.benhsoan.port.dto.result.PatientPortalRegistrationResult;
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
class PatientRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private LoginUseCase loginUseCase;
    @MockitoBean private LogoutUseCase logoutUseCase;
    @MockitoBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockitoBean private PatientLoginUseCase patientLoginUseCase;
    @MockitoBean private PatientPortalRegistrationUseCase patientPortalRegistrationUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void registersNewPatientReturns201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        when(patientPortalRegistrationUseCase.register(any(PatientPortalRegistrationCommand.class)))
                .thenReturn(new PatientPortalRegistrationResult(
                        userId, patientId, "0345678910", "Nguyen Van A"));

        mockMvc.perform(post("/auth/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "0345678910",
                                  "password": "secret",
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1990-01-01",
                                  "gender": "FEMALE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.phone").value("0345678910"))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"));
    }

    @Test
    void duplicatePhoneReturns409() throws Exception {
        when(patientPortalRegistrationUseCase.register(any(PatientPortalRegistrationCommand.class)))
                .thenThrow(new PhoneAlreadyExistsException(
                        "Số điện thoại đã được đăng ký tài khoản. Vui lòng đăng nhập."));

        mockMvc.perform(post("/auth/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "0345678910",
                                  "password": "secret",
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1990-01-01",
                                  "gender": "FEMALE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_ALREADY_EXISTS"));
    }

    @Test
    void invalidPhoneReturns400() throws Exception {
        when(patientPortalRegistrationUseCase.register(any(PatientPortalRegistrationCommand.class)))
                .thenThrow(new ValidationException("Số điện thoại không hợp lệ."));

        mockMvc.perform(post("/auth/patient/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "12345",
                                  "password": "secret",
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1990-01-01",
                                  "gender": "FEMALE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
