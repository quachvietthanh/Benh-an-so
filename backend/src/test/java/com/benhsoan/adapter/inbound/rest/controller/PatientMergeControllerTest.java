package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
import com.benhsoan.domain.patient.exception.PatientAlreadyMergedException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.dto.result.patient.DuplicatePatientGroupResult;
import com.benhsoan.port.dto.result.patient.MergePatientsResult;
import com.benhsoan.port.inbound.patient.FindDuplicatePatientsUseCase;
import com.benhsoan.port.inbound.patient.GetPatientByCodeUseCase;
import com.benhsoan.port.inbound.patient.GetPatientByIdUseCase;
import com.benhsoan.port.inbound.patient.MergePatientsUseCase;
import com.benhsoan.port.inbound.patient.RegisterPatientUseCase;
import com.benhsoan.port.inbound.patient.SearchPatientUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientPregnancyStatusUseCase;
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
        PatientMergeControllerTest.AspectTestConfig.class
})
@DisplayName("Patient Merge API Controller - Integration Tests (NCL-02-CN-006 / TC-01, TC-03, TC-04)")
class PatientMergeControllerTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {}

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterPatientUseCase registerPatientUseCase;
    @MockitoBean private SearchPatientUseCase searchPatientUseCase;
    @MockitoBean private UpdatePatientUseCase updatePatientUseCase;
    @MockitoBean private UpdatePatientPregnancyStatusUseCase updatePatientPregnancyStatusUseCase;
    @MockitoBean private GetPatientByIdUseCase getPatientByIdUseCase;
    @MockitoBean private GetPatientByCodeUseCase getPatientByCodeUseCase;
    @MockitoBean private MergePatientsUseCase mergePatientsUseCase;
    @MockitoBean private FindDuplicatePatientsUseCase findDuplicatePatientsUseCase;
    @MockitoBean private com.benhsoan.port.inbound.patient.DownloadPatientImportTemplateUseCase downloadPatientImportTemplateUseCase;
    @MockitoBean private com.benhsoan.port.inbound.patient.PreviewPatientImportUseCase previewPatientImportUseCase;
    @MockitoBean private com.benhsoan.port.inbound.patient.ImportPatientsUseCase importPatientsUseCase;
    @MockitoBean private com.benhsoan.port.inbound.patient.GetPatientImportLogsUseCase getPatientImportLogsUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    @DisplayName("NCL-02-CN-006-TC-01: Gộp hồ sơ thành công trả về 200 OK và thông tin gộp")
    void mergePatientsSucceedsForReceptionist() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        MergePatientsResult result = new MergePatientsResult(
                sourceId, "BN000001", targetId, "BN000002", 4, operatorId, "Hồ sơ trùng tiếp đón", Instant.now()
        );
        when(mergePatientsUseCase.merge(any())).thenReturn(result);

        String requestBody = """
                {
                    "sourcePatientId": "%s",
                    "targetPatientId": "%s",
                    "reason": "Hồ sơ trùng tiếp đón"
                }
                """.formatted(sourceId, targetId);

        mockMvc.perform(post("/patients/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_MERGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourcePatientId").value(sourceId.toString()))
                .andExpect(jsonPath("$.sourcePatientCode").value("BN000001"))
                .andExpect(jsonPath("$.targetPatientId").value(targetId.toString()))
                .andExpect(jsonPath("$.targetPatientCode").value("BN000002"))
                .andExpect(jsonPath("$.transferredVisitsCount").value(4));
    }

    @Test
    @DisplayName("NCL-02-CN-006: Validate lỗi 400 Bad Request khi thiếu sourcePatientId hoặc targetPatientId")
    void mergePatientsValidatesMissingFields() throws Exception {
        String invalidBody = """
                {
                    "sourcePatientId": null,
                    "targetPatientId": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/patients/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_MERGE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("NCL-02-CN-006-TC-04: Bác sĩ gọi merge bị từ chối 403 Forbidden")
    void mergePatientsForbiddenForDoctor() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        String requestBody = """
                {
                    "sourcePatientId": "%s",
                    "targetPatientId": "%s",
                    "reason": "Bác sĩ thử gộp"
                }
                """.formatted(sourceId, targetId);

        mockMvc.perform(post("/patients/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(user("doctor").roles("DOCTOR").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NCL-02-CN-006-TC-03: Cập nhật hồ sơ đã gộp bị từ chối 409 Conflict")
    void updateMergedPatientReturnsConflict() throws Exception {
        UUID mergedPatientId = UUID.randomUUID();
        UUID retainedPatientId = UUID.randomUUID();

        when(updatePatientUseCase.update(any(), any()))
                .thenThrow(new PatientAlreadyMergedException("BN000001", retainedPatientId));

        String updateBody = """
                {
                    "fullName": "Nguyen Van A",
                    "dateOfBirth": "1990-01-01",
                    "gender": "MALE",
                    "phone": "0901234567",
                    "active": true,
                    "consentAgreed": true,
                    "consentVersion": "v1.0"
                }
                """;

        mockMvc.perform(put("/patients/{patientId}", mergedPatientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.mergedIntoPatientId").value(retainedPatientId.toString()));
    }

    @Test
    @DisplayName("NCL-02-CN-006: Xem danh sách hồ sơ nghi trùng trả về 200 OK")
    void findDuplicatesSucceedsForStaffWithPatientRead() throws Exception {
        PatientResult candidate1 = new PatientResult(
                UUID.randomUUID(), "BN000001", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                Gender.MALE, "0901234567", null, "HN", null, null, null, null, null, true,
                Instant.now(), Instant.now(), true, Instant.now(), "v1.0", false, null, null, false
        );
        DuplicatePatientGroupResult group = new DuplicatePatientGroupResult(
                "Nguyen Van A", LocalDate.of(1990, 1, 1), "0901234567", List.of(candidate1)
        );
        when(findDuplicatePatientsUseCase.findDuplicates()).thenReturn(List.of(group));

        mockMvc.perform(get("/patients/duplicates")
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$[0].phone").value("0901234567"))
                .andExpect(jsonPath("$[0].candidates[0].patientCode").value("BN000001"));
    }
}
