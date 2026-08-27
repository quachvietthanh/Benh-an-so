package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.command.patient.RegisterPatientCommand;
import com.benhsoan.port.dto.command.patient.UpdatePatientCommand;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.GetPatientByCodeUseCase;
import com.benhsoan.port.inbound.patient.GetPatientByIdUseCase;
import com.benhsoan.port.inbound.patient.RegisterPatientUseCase;
import com.benhsoan.port.inbound.patient.SearchPatientUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientController.class)
@Import({
        PatientRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        PatientConsentIntegrationTest.AspectTestConfig.class
})
@DisplayName("User Story NCL-15-CN-001 - Personal Data Processing Consent Acceptance Tests")
class PatientConsentIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterPatientUseCase registerPatientUseCase;
    @MockitoBean private SearchPatientUseCase searchPatientUseCase;
    @MockitoBean private UpdatePatientUseCase updatePatientUseCase;
    @MockitoBean private GetPatientByIdUseCase getPatientByIdUseCase;
    @MockitoBean private GetPatientByCodeUseCase getPatientByCodeUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    @DisplayName("NCL-15-CN-001-TC-01: Ghi nhận phiếu đồng ý thành công khi lập hồ sơ mới")
    void recordsConsentSuccessfullyOnRegistration() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();

        PatientResult result = new PatientResult(
                patientId,
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1995, 5, 10),
                Gender.MALE,
                "0909000001",
                "a@example.com",
                "123 Street",
                "079095001234",
                "DN4790123456789",
                null,
                "Nguyen Van B",
                "0909998877",
                true,
                now,
                now,
                true,
                now,
                "v1.0",
                false,
                null,
                null,
                false
        );

        when(registerPatientUseCase.register(any(RegisterPatientCommand.class))).thenReturn(result);

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "email": "a@example.com",
                                  "address": "123 Street",
                                  "identityNumber": "079095001234",
                                  "consentAgreed": true,
                                  "consentVersion": "v1.0"
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.patientCode").value("BN000001"))
                .andExpect(jsonPath("$.consentAgreed").value(true))
                .andExpect(jsonPath("$.consentVersion").value("v1.0"))
                .andExpect(jsonPath("$.consentWithdrawn").value(false))
                .andExpect(jsonPath("$.nonMedicalUseRestricted").value(false));

        verify(registerPatientUseCase).register(any(RegisterPatientCommand.class));
    }

    @Test
    @DisplayName("NCL-15-CN-001-TC-02 / QTN-24: Chặn lưu hồ sơ mới khi consentAgreed = false")
    void rejectsRegistrationWhenConsentAgreedIsFalse() throws Exception {
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "consentAgreed": false
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields.consentAgreed").exists());
    }

    @Test
    @DisplayName("NCL-15-CN-001-TC-02 / QTN-24: Chặn lưu hồ sơ mới khi thiếu trường consentAgreed")
    void rejectsRegistrationWhenConsentAgreedIsMissing() throws Exception {
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001"
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields.consentAgreed").exists());
    }

    @Test
    @DisplayName("NCL-15-CN-001-TC-03: Ghi nhận rút lại sự đồng ý, cập nhật nonMedicalUseRestricted = true")
    void withdrawsConsentSuccessfullyViaPut() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();

        PatientResult result = new PatientResult(
                patientId,
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1995, 5, 10),
                Gender.MALE,
                "0909000001",
                "a@example.com",
                "123 Street",
                "079095001234",
                "DN4790123456789",
                null,
                "Nguyen Van B",
                "0909998877",
                true,
                now,
                now,
                true,
                now,
                "v1.0",
                true,
                now,
                "Người bệnh yêu cầu rút lại sự đồng ý",
                true
        );

        when(updatePatientUseCase.update(any(UUID.class), any(UpdatePatientCommand.class))).thenReturn(result);

        mockMvc.perform(put("/patients/{patientId}", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "active": true,
                                  "consentWithdrawn": true,
                                  "consentWithdrawnReason": "Người bệnh yêu cầu rút lại sự đồng ý"
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentWithdrawn").value(true))
                .andExpect(jsonPath("$.consentWithdrawnReason").value("Người bệnh yêu cầu rút lại sự đồng ý"))
                .andExpect(jsonPath("$.nonMedicalUseRestricted").value(true))
                .andExpect(jsonPath("$.active").value(true));

        verify(updatePatientUseCase).update(any(UUID.class), any(UpdatePatientCommand.class));
    }
}
