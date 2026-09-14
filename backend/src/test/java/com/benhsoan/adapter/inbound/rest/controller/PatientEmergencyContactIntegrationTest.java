package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.shared.exception.ValidationException;
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
        AnonymizationModeState.class,
        PatientRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        PatientEmergencyContactIntegrationTest.AspectTestConfig.class
})
@DisplayName("User Story NCL-02-CN-007 - Patient Emergency Contact Integration Tests")
class PatientEmergencyContactIntegrationTest {

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
    @DisplayName("TC-01: Lưu thành công người liên hệ khẩn cấp (họ tên, quan hệ, SĐT) khi đăng ký hồ sơ")
    void registersPatientWithEmergencyContactReturnsSuccess() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();

        PatientResult result = new PatientResult(
                patientId, "BN000001", "Nguyen Van A",
                LocalDate.of(1995, 5, 10), Gender.MALE, "0909000001",
                "a@example.com", "123 Street", "079095001234", "DN4790123456789",
                null, "Le Thi B", "Vợ", "0909998877",
                true, now, now, true, now, "v1.0", false, null, null, false
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
                                  "emergencyContact": "Le Thi B",
                                  "emergencyRelationship": "Vợ",
                                  "emergencyPhone": "0909998877",
                                  "consentAgreed": true,
                                  "consentVersion": "v1.0"
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.emergencyContact").value("Le Thi B"))
                .andExpect(jsonPath("$.emergencyRelationship").value("Vợ"))
                .andExpect(jsonPath("$.emergencyPhone").value("0909998877"));
    }

    @Test
    @DisplayName("TC-02: Từ chối lưu khi số điện thoại người liên hệ khẩn cấp sai định dạng")
    void rejectsRegistrationWhenEmergencyPhoneFormatIsInvalid() throws Exception {
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "emergencyContact": "Le Thi B",
                                  "emergencyRelationship": "Vợ",
                                  "emergencyPhone": "01234567890",
                                  "consentAgreed": true,
                                  "consentVersion": "v1.0"
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields.emergencyPhone").exists());
    }

    @Test
    @DisplayName("TC-01 & TC-03: Cập nhật người liên hệ khẩn cấp thành công")
    void updatesEmergencyContactSuccessfully() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();

        PatientResult result = new PatientResult(
                patientId, "BN000001", "Nguyen Van A",
                LocalDate.of(1995, 5, 10), Gender.MALE, "0909000001",
                "a@example.com", "123 Street", "079095001234", "DN4790123456789",
                null, "Tran Van C", "Bố", "0908887766",
                true, now, now, true, now, "v1.0", false, null, null, false
        );

        when(updatePatientUseCase.update(eq(patientId), any(UpdatePatientCommand.class))).thenReturn(result);

        mockMvc.perform(put("/patients/" + patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "emergencyContact": "Tran Van C",
                                  "emergencyRelationship": "Bố",
                                  "emergencyPhone": "0908887766",
                                  "active": true
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emergencyContact").value("Tran Van C"))
                .andExpect(jsonPath("$.emergencyRelationship").value("Bố"))
                .andExpect(jsonPath("$.emergencyPhone").value("0908887766"));
    }

    @Test
    @DisplayName("TC-02: Từ chối cập nhật khi số điện thoại người liên hệ khẩn cấp sai định dạng")
    void rejectsUpdateWhenEmergencyPhoneFormatIsInvalid() throws Exception {
        UUID patientId = UUID.randomUUID();

        mockMvc.perform(put("/patients/" + patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "emergencyContact": "Tran Van C",
                                  "emergencyRelationship": "Bố",
                                  "emergencyPhone": "invalid-phone",
                                  "active": true
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields.emergencyPhone").exists());
    }

    @Test
    @DisplayName("P1.01: Cho phép cập nhật khi gửi số điện thoại người liên hệ khẩn cấp dạng che ẩn danh")
    void updatesEmergencyContactWithMaskedPhoneSuccessfully() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();

        PatientResult result = new PatientResult(
                patientId, "BN000001", "Nguyen Van A",
                LocalDate.of(1995, 5, 10), Gender.MALE, "0909000001",
                "a@example.com", "123 Street", "079095001234", "DN4790123456789",
                null, "Tran Van C", "Bố", "0908887766",
                true, now, now, true, now, "v1.0", false, null, null, false
        );

        when(updatePatientUseCase.update(eq(patientId), any(UpdatePatientCommand.class))).thenReturn(result);

        mockMvc.perform(put("/patients/" + patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "emergencyContact": "Tran Van C",
                                  "emergencyRelationship": "Bố",
                                  "emergencyPhone": "09******66",
                                  "active": true
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emergencyPhone").value("0908887766"));
    }

    @Test
    @DisplayName("P1.02: Cho phép đăng ký khi người liên hệ khẩn cấp để chuỗi rỗng")
    void registersPatientWithEmptyEmergencyContactFieldsSuccessfully() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();

        PatientResult result = new PatientResult(
                patientId, "BN000001", "Nguyen Van A",
                LocalDate.of(1995, 5, 10), Gender.MALE, "0909000001",
                "a@example.com", "123 Street", "079095001234", "DN4790123456789",
                null, null, null, null,
                true, now, now, true, now, "v1.0", false, null, null, false
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
                                  "emergencyContact": "",
                                  "emergencyRelationship": "",
                                  "emergencyPhone": "",
                                  "consentAgreed": true,
                                  "consentVersion": "v1.0"
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("P1.02: Cho phép cập nhật khi người liên hệ khẩn cấp để chuỗi rỗng")
    void updatesPatientWithEmptyEmergencyPhoneSuccessfully() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();

        PatientResult result = new PatientResult(
                patientId, "BN000001", "Nguyen Van A",
                LocalDate.of(1995, 5, 10), Gender.MALE, "0909000001",
                "a@example.com", "123 Street", "079095001234", "DN4790123456789",
                null, null, null, null,
                true, now, now, true, now, "v1.0", false, null, null, false
        );

        when(updatePatientUseCase.update(eq(patientId), any(UpdatePatientCommand.class))).thenReturn(result);

        mockMvc.perform(put("/patients/" + patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "emergencyContact": "",
                                  "emergencyRelationship": "",
                                  "emergencyPhone": "",
                                  "active": true
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("P3.01: Từ chối đăng ký khi quan hệ nhân thân vượt quá 50 ký tự")
    void rejectsRegistrationWhenEmergencyRelationshipExceeds50Chars() throws Exception {
        String longRelationship = "A".repeat(51);

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "emergencyContact": "Le Thi B",
                                  "emergencyRelationship": "%s",
                                  "emergencyPhone": "0909998877",
                                  "consentAgreed": true,
                                  "consentVersion": "v1.0"
                                }
                                """.formatted(longRelationship))
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields.emergencyRelationship").exists());
    }

    @Test
    @DisplayName("P3.01: Từ chối cập nhật khi quan hệ nhân thân vượt quá 50 ký tự")
    void rejectsUpdateWhenEmergencyRelationshipExceeds50Chars() throws Exception {
        UUID patientId = UUID.randomUUID();
        String longRelationship = "A".repeat(51);

        mockMvc.perform(put("/patients/" + patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "emergencyContact": "Tran Van C",
                                  "emergencyRelationship": "%s",
                                  "emergencyPhone": "0908887766",
                                  "active": true
                                }
                                """.formatted(longRelationship))
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields.emergencyRelationship").exists());
    }

    @Test
    @DisplayName("P2.02: Trả về details.fields khi Use Case ném ValidationException với tiền tố field")
    void returnsDetailsFieldsWhenUseCaseThrowsValidationException() throws Exception {
        UUID patientId = UUID.randomUUID();

        when(updatePatientUseCase.update(eq(patientId), any(UpdatePatientCommand.class)))
                .thenThrow(new ValidationException(
                        "emergencyRelationship: Mối quan hệ với người liên hệ khẩn cấp không được để trống."
                ));

        mockMvc.perform(put("/patients/" + patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van A",
                                  "dateOfBirth": "1995-05-10",
                                  "gender": "MALE",
                                  "phone": "0909000001",
                                  "emergencyContact": "Tran Van C",
                                  "emergencyRelationship": "Bố",
                                  "emergencyPhone": "0908887766",
                                  "active": true
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields.emergencyRelationship")
                        .value("Mối quan hệ với người liên hệ khẩn cấp không được để trống."));
    }
}
