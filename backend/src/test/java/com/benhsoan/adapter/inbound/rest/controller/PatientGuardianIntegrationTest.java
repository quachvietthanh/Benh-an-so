package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        PatientGuardianIntegrationTest.AspectTestConfig.class
})
@DisplayName("User Story NCL-02-CN-008 - Pediatric Patient Guardian Integration Tests")
class PatientGuardianIntegrationTest {

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
    @Autowired private AnonymizationModeState anonymizationModeState;

    @Test
    @DisplayName("TC-01 & TC-03: Đăng ký thành công bệnh nhân trẻ em gắn người giám hộ và đứng tên phiếu đồng ý")
    void registersPediatricPatientWithGuardianSuccessfully() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();
        LocalDate childDob = LocalDate.of(2020, 1, 1);

        PatientResult result = new PatientResult(
                patientId, "BN000008", "Nguyen Van Con",
                childDob, Gender.MALE, null,
                null, "123 Le Loi, Da Nang", null, null,
                null, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null,
                "Nguyen Van Bo", true, false,
                true, now, now, true, now, "v1.0", false, null, null, false
        );

        when(registerPatientUseCase.register(any(RegisterPatientCommand.class))).thenReturn(result);

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van Con",
                                  "dateOfBirth": "2020-01-01",
                                  "gender": "MALE",
                                  "address": "123 Le Loi, Da Nang",
                                  "guardianName": "Nguyen Van Bo",
                                  "guardianRelationship": "Bố",
                                  "guardianPhone": "0912345678",
                                  "guardianIdentityNumber": "001200000001",
                                  "consentAgreed": true,
                                  "consentVersion": "v1.0"
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van Con"))
                .andExpect(jsonPath("$.isMinor").value(true))
                .andExpect(jsonPath("$.requiresAdultTransitionPrompt").value(false))
                .andExpect(jsonPath("$.guardianName").value("Nguyen Van Bo"))
                .andExpect(jsonPath("$.guardianRelationship").value("Bố"))
                .andExpect(jsonPath("$.guardianPhone").value("0912345678"))
                .andExpect(jsonPath("$.guardianIdentityNumber").value("001200000001"))
                .andExpect(jsonPath("$.consentSignerName").value("Nguyen Van Bo"));
    }

    @Test
    @DisplayName("TC-02: Từ chối đăng ký trẻ em khi use case báo lỗi thiếu người giám hộ")
    void rejectsRegistrationWhenGuardianIsMissing() throws Exception {
        when(registerPatientUseCase.register(any(RegisterPatientCommand.class)))
                .thenThrow(new ValidationException("guardianName", "Hồ sơ bệnh nhân dưới 18 tuổi bắt buộc phải khai báo người giám hộ (QTN-44)."));

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van Con",
                                  "dateOfBirth": "2020-01-01",
                                  "gender": "MALE",
                                  "guardianRelationship": "Bố",
                                  "guardianPhone": "0912345678",
                                  "consentAgreed": true,
                                  "consentVersion": "v1.0"
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields.guardianName").exists());
    }

    @Test
    @DisplayName("TC-02: Từ chối đăng ký trẻ em khi số điện thoại người giám hộ sai định dạng")
    void rejectsRegistrationWhenGuardianPhoneIsInvalid() throws Exception {
        when(registerPatientUseCase.register(any(RegisterPatientCommand.class)))
                .thenThrow(new ValidationException("guardianPhone", "Số điện thoại người giám hộ không đúng định dạng."));

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van Con",
                                  "dateOfBirth": "2020-01-01",
                                  "gender": "MALE",
                                  "guardianName": "Nguyen Van Bo",
                                  "guardianRelationship": "Bố",
                                  "guardianPhone": "12345",
                                  "consentAgreed": true,
                                  "consentVersion": "v1.0"
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields.guardianPhone").exists());
    }

    @Test
    @DisplayName("Truy vấn hồ sơ bệnh nhân trẻ em trả về thông tin người giám hộ đầy đủ")
    void getsPediatricPatientByIdSuccessfully() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();
        LocalDate childDob = LocalDate.of(2020, 1, 1);

        PatientResult result = new PatientResult(
                patientId, "BN000008", "Nguyen Van Con",
                childDob, Gender.MALE, null,
                null, "123 Le Loi, Da Nang", null, null,
                null, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null,
                "Nguyen Van Bo", true, false,
                true, now, now, true, now, "v1.0", false, null, null, false
        );

        when(getPatientByIdUseCase.getById(eq(patientId))).thenReturn(result);

        mockMvc.perform(get("/patients/" + patientId)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.guardianName").value("Nguyen Van Bo"))
                .andExpect(jsonPath("$.guardianRelationship").value("Bố"))
                .andExpect(jsonPath("$.guardianPhone").value("0912345678"))
                .andExpect(jsonPath("$.consentSignerName").value("Nguyen Van Bo"))
                .andExpect(jsonPath("$.isMinor").value(true));
    }

    @Test
    @DisplayName("Cập nhật thông tin người giám hộ cho bệnh nhân trẻ em thành công")
    void updatesGuardianSuccessfully() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();
        LocalDate childDob = LocalDate.of(2020, 1, 1);

        PatientResult result = new PatientResult(
                patientId, "BN000008", "Nguyen Van Con",
                childDob, Gender.MALE, null,
                null, "123 Le Loi, Da Nang", null, null,
                null, null, null, null,
                "Nguyen Thi Me", "Mẹ", "0987654321", "001200000002", null,
                "Nguyen Thi Me", true, false,
                true, now, now, true, now, "v1.0", false, null, null, false
        );

        when(updatePatientUseCase.update(eq(patientId), any(UpdatePatientCommand.class))).thenReturn(result);

        mockMvc.perform(put("/patients/" + patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van Con",
                                  "dateOfBirth": "2020-01-01",
                                  "gender": "MALE",
                                  "guardianName": "Nguyen Thi Me",
                                  "guardianRelationship": "Mẹ",
                                  "guardianPhone": "0987654321",
                                  "guardianIdentityNumber": "001200000002",
                                  "active": true
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardianName").value("Nguyen Thi Me"))
                .andExpect(jsonPath("$.guardianRelationship").value("Mẹ"))
                .andExpect(jsonPath("$.guardianPhone").value("0987654321"))
                .andExpect(jsonPath("$.consentSignerName").value("Nguyen Thi Me"));
    }

    @Test
    @DisplayName("TC-04: Bệnh nhân đủ 18 tuổi chuyển tiếp thành niên thành công qua API PUT /patients/{id}")
    void transitionsToAdultSuccessfully() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();
        LocalDate adultDob = LocalDate.of(2005, 1, 1);

        PatientResult result = new PatientResult(
                patientId, "BN000009", "Nguyen Van Truong Thanh",
                adultDob, Gender.MALE, "0901234567",
                "tt@example.com", "123 Le Loi, Da Nang", "079095009999", null,
                null, null, null, null,
                null, null, null, null, null,
                "Nguyen Van Truong Thanh", false, false,
                true, now, now, true, now, "v1.0", false, null, null, false
        );

        when(updatePatientUseCase.update(eq(patientId), any(UpdatePatientCommand.class))).thenReturn(result);

        mockMvc.perform(put("/patients/" + patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van Truong Thanh",
                                  "dateOfBirth": "2005-01-01",
                                  "gender": "MALE",
                                  "phone": "0901234567",
                                  "email": "tt@example.com",
                                  "address": "123 Le Loi, Da Nang",
                                  "identityNumber": "079095009999",
                                  "transitionToAdult": true,
                                  "active": true
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMinor").value(false))
                .andExpect(jsonPath("$.requiresAdultTransitionPrompt").value(false))
                .andExpect(jsonPath("$.guardianName").doesNotExist())
                .andExpect(jsonPath("$.guardianPhone").doesNotExist())
                .andExpect(jsonPath("$.consentSignerName").value("Nguyen Van Truong Thanh"));
    }

    @Test
    @DisplayName("TC-04: Từ chối chuyển tiếp thành niên khi bệnh nhân chưa đủ 18 tuổi")
    void rejectsTransitionToAdultWhenMinor() throws Exception {
        UUID patientId = UUID.randomUUID();

        when(updatePatientUseCase.update(eq(patientId), any(UpdatePatientCommand.class)))
                .thenThrow(new ValidationException("transitionToAdult", "Bệnh nhân chưa đủ 18 tuổi, không thể chuyển sang tự chịu trách nhiệm."));

        mockMvc.perform(put("/patients/" + patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van Con",
                                  "dateOfBirth": "2020-01-01",
                                  "gender": "MALE",
                                  "transitionToAdult": true,
                                  "active": true
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields.transitionToAdult").exists());
    }

    @Test
    @DisplayName("P1-2: Từ chối đăng ký bệnh nhân trẻ em khi consentSignerName khác người giám hộ")
    void rejectsRegistrationWhenConsentSignerMismatchGuardian() throws Exception {
        when(registerPatientUseCase.register(any(RegisterPatientCommand.class)))
                .thenThrow(new ValidationException("consentSignerName", "Đối với bệnh nhân chưa thành niên, người ký phiếu đồng ý bắt buộc phải là người giám hộ (QTN-44)."));

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van Con",
                                  "dateOfBirth": "2020-01-01",
                                  "gender": "MALE",
                                  "guardianName": "Nguyen Van Bo",
                                  "guardianRelationship": "Bố",
                                  "guardianPhone": "0912345678",
                                  "consentSignerName": "Nguoi La",
                                  "consentAgreed": true,
                                  "consentVersion": "v1.0"
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields.consentSignerName").exists());
    }

    @Test
    @DisplayName("P2-1: Từ chối gỡ người giám hộ khi bệnh nhân đủ 18 tuổi nếu không có transitionToAdult")
    void rejectsClearingGuardianForAdultWithoutTransitionToAdult() throws Exception {
        UUID patientId = UUID.randomUUID();

        when(updatePatientUseCase.update(eq(patientId), any(UpdatePatientCommand.class)))
                .thenThrow(new ValidationException("transitionToAdult", "Bệnh nhân đã đủ 18 tuổi. Việc gỡ bỏ người giám hộ yêu cầu kích hoạt quy trình chuyển tiếp thành niên (transitionToAdult = true) để ký gia hạn phiếu đồng ý mới (TC-04)."));

        mockMvc.perform(put("/patients/" + patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Van Truong Thanh",
                                  "dateOfBirth": "2005-01-01",
                                  "gender": "MALE",
                                  "active": true
                                }
                                """)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.fields.transitionToAdult").exists());
    }

    @Test
    @DisplayName("P3-2: Khi anonymization bật, guardianName và minor consentSignerName có nhãn riêng GIÁM HỘ #BN...")
    void masksGuardianWithDistinctGuardianLabelWhenAnonymizationEnabled() throws Exception {
        UUID patientId = UUID.randomUUID();
        Instant now = Instant.now();
        LocalDate childDob = LocalDate.of(2020, 1, 1);

        PatientResult result = new PatientResult(
                patientId, "BN000008", "Nguyen Van Con",
                childDob, Gender.MALE, null,
                null, "123 Le Loi, Da Nang", null, null,
                null, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null,
                "Nguyen Van Bo", true, false,
                true, now, now, true, now, "v1.0", false, null, null, false
        );

        when(getPatientByIdUseCase.getById(eq(patientId))).thenReturn(result);

        anonymizationModeState.setEnabled(true);
        try {
            mockMvc.perform(get("/patients/" + patientId)
                            .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("BỆNH NHÂN #BN000008"))
                    .andExpect(jsonPath("$.guardianName").value("GIÁM HỘ #BN000008"))
                    .andExpect(jsonPath("$.consentSignerName").value("GIÁM HỘ #BN000008"));
        } finally {
            anonymizationModeState.setEnabled(false);
        }
    }
}
