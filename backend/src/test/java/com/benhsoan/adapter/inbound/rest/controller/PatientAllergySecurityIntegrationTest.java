package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.benhsoan.adapter.inbound.rest.mapper.PatientAllergyRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.patient.exception.PatientAllergyAlreadyExistsException;
import com.benhsoan.domain.patient.exception.PatientAllergyNotFoundException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.patient.PatientAllergyChangeLogResult;
import com.benhsoan.port.dto.result.patient.PatientAllergyResult;
import com.benhsoan.port.inbound.patient.AddPatientAllergyUseCase;
import com.benhsoan.port.inbound.patient.DeletePatientAllergyUseCase;
import com.benhsoan.port.inbound.patient.GetPatientAllergiesUseCase;
import com.benhsoan.port.inbound.patient.GetPatientAllergyChangeLogsUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientAllergyUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientAllergyController.class)
@Import({
        PatientAllergyRestMapper.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        GlobalExceptionHandler.class,
        PatientAllergySecurityIntegrationTest.AspectTestConfig.class
})
class PatientAllergySecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AddPatientAllergyUseCase addPatientAllergyUseCase;
    @MockitoBean private UpdatePatientAllergyUseCase updatePatientAllergyUseCase;
    @MockitoBean private DeletePatientAllergyUseCase deletePatientAllergyUseCase;
    @MockitoBean private GetPatientAllergiesUseCase getPatientAllergiesUseCase;
    @MockitoBean private GetPatientAllergyChangeLogsUseCase getPatientAllergyChangeLogsUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    private final UUID patientId = UUID.randomUUID();
    private final UUID allergyId = UUID.randomUUID();

    @Test
    @DisplayName("TC-01: Bác sĩ có quyền PATIENT_ALLERGY_WRITE thêm dị ứng -> 201 Created")
    void tc01_doctorAddsAllergy_shouldReturnCreated() throws Exception {
        PatientAllergyResult result = PatientAllergyResult.builder()
                .id(allergyId)
                .patientId(patientId)
                .allergenType("MEDICATION")
                .allergenName("Penicillin")
                .severity(AllergySeverity.SEVERE)
                .reaction("Mề đay, khó thở nhẹ")
                .notes("Ghi nhận 2022")
                .active(true)
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.parse("2026-09-07T08:00:00Z"))
                .updatedAt(Instant.parse("2026-09-07T08:00:00Z"))
                .build();

        when(addPatientAllergyUseCase.addAllergy(any())).thenReturn(result);

        String payload = """
                {
                    "allergenType": "MEDICATION",
                    "allergenName": "Penicillin",
                    "severity": "SEVERE",
                    "reaction": "Mề đay, khó thở nhẹ",
                    "notes": "Ghi nhận 2022"
                }
                """;

        mockMvc.perform(post("/patients/{patientId}/allergies", patientId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_ALLERGY_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(allergyId.toString()))
                .andExpect(jsonPath("$.allergenName").value("Penicillin"))
                .andExpect(jsonPath("$.severity").value("SEVERE"));
    }

    @Test
    @DisplayName("TC-02: Thêm hoạt chất trùng lặp -> 409 Conflict")
    void tc02_duplicateAllergen_shouldReturnConflict() throws Exception {
        when(addPatientAllergyUseCase.addAllergy(any()))
                .thenThrow(new PatientAllergyAlreadyExistsException("Penicillin"));

        String payload = """
                {
                    "allergenName": "Penicillin",
                    "severity": "SEVERE"
                }
                """;

        mockMvc.perform(post("/patients/{patientId}/allergies", patientId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_ALLERGY_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PATIENT_ALLERGY_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("TC-03: Lễ tân không có quyền PATIENT_ALLERGY_WRITE -> 403 Forbidden và ghi Audit Log")
    void tc03_receptionistAddsAllergy_shouldReturnForbidden() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        String payload = """
                {
                    "allergenName": "Penicillin",
                    "severity": "MILD"
                }
                """;

        // Lễ tân có PATIENT_UPDATE nhưng KHÔNG có PATIENT_ALLERGY_WRITE
        mockMvc.perform(post("/patients/{patientId}/allergies", patientId)
                        .with(user("receptionist").authorities(
                                new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"),
                                new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("TC-04: Cập nhật mục dị ứng -> 200 OK")
    void tc04_updateAllergy_shouldReturnOk() throws Exception {
        PatientAllergyResult result = PatientAllergyResult.builder()
                .id(allergyId)
                .patientId(patientId)
                .allergenType("MEDICATION")
                .allergenName("Penicillin G")
                .severity(AllergySeverity.ANAPHYLAXIS)
                .reaction("Phản vệ nặng")
                .active(true)
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.parse("2026-09-07T08:00:00Z"))
                .updatedBy(UUID.randomUUID())
                .updatedAt(Instant.parse("2026-09-07T08:30:00Z"))
                .build();

        when(updatePatientAllergyUseCase.updateAllergy(any())).thenReturn(result);

        String payload = """
                {
                    "allergenName": "Penicillin G",
                    "severity": "ANAPHYLAXIS",
                    "reaction": "Phản vệ nặng",
                    "changeReason": "Bổ sung diễn tiến nặng"
                }
                """;

        mockMvc.perform(put("/patients/{patientId}/allergies/{allergyId}", patientId, allergyId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_ALLERGY_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergenName").value("Penicillin G"))
                .andExpect(jsonPath("$.severity").value("ANAPHYLAXIS"));
    }

    @Test
    @DisplayName("TC-04: Xóa mục dị ứng -> 204 No Content")
    void tc04_deleteAllergy_shouldReturnNoContent() throws Exception {
        doNothing().when(deletePatientAllergyUseCase).deleteAllergy(any());

        mockMvc.perform(delete("/patients/{patientId}/allergies/{allergyId}", patientId, allergyId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_ALLERGY_WRITE")))
                        .param("reason", "Chẩn đoán lại"))
                .andExpect(status().isNoContent());

        verify(deletePatientAllergyUseCase).deleteAllergy(any());
    }

    @Test
    @DisplayName("Tra cứu danh sách dị ứng với quyền PATIENT_ALLERGY_READ -> 200 OK")
    void getAllergies_withReadPermission_shouldReturnOk() throws Exception {
        PatientAllergyResult item = PatientAllergyResult.builder()
                .id(allergyId)
                .patientId(patientId)
                .allergenType("MEDICATION")
                .allergenName("Aspirin")
                .severity(AllergySeverity.MILD)
                .active(true)
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(getPatientAllergiesUseCase.getAllergies(patientId)).thenReturn(List.of(item));

        mockMvc.perform(get("/patients/{patientId}/allergies", patientId)
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_ALLERGY_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].allergenName").value("Aspirin"));
    }

    @Test
    @DisplayName("Tra cứu lịch sử thay đổi của mục dị ứng (TC-04) với PATIENT_ALLERGY_WRITE -> 200 OK và serialize JSON Object")
    void getChangeLogs_shouldReturnHistory() throws Exception {
        PatientAllergyChangeLogResult log = PatientAllergyChangeLogResult.builder()
                .id(UUID.randomUUID())
                .allergyId(allergyId)
                .patientId(patientId)
                .action("UPDATE")
                .beforeData("{\"severity\":\"MILD\",\"allergenName\":\"Penicillin\"}")
                .afterData("{\"severity\":\"SEVERE\",\"allergenName\":\"Penicillin\"}")
                .changeReason("Bổ sung mức độ nặng")
                .changedBy(UUID.randomUUID())
                .changedAt(Instant.now())
                .build();

        when(getPatientAllergyChangeLogsUseCase.getChangeLogs(patientId, allergyId)).thenReturn(List.of(log));

        mockMvc.perform(get("/patients/{patientId}/allergies/{allergyId}/history", patientId, allergyId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_ALLERGY_WRITE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("UPDATE"))
                .andExpect(jsonPath("$[0].changeReason").value("Bổ sung mức độ nặng"))
                .andExpect(jsonPath("$[0].beforeData.severity").value("MILD"))
                .andExpect(jsonPath("$[0].beforeData.allergenName").value("Penicillin"))
                .andExpect(jsonPath("$[0].afterData.severity").value("SEVERE"));
    }

    @Test
    @DisplayName("P1-02: Y tá có PATIENT_ALLERGY_READ gọi GET history -> 403 Forbidden")
    void getChangeLogs_nurse_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/patients/{patientId}/allergies/{allergyId}/history", patientId, allergyId)
                        .with(user("nurse").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_ALLERGY_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P1-02: Dược sĩ có PATIENT_ALLERGY_READ gọi GET history -> 403 Forbidden")
    void getChangeLogs_pharmacist_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/patients/{patientId}/allergies/{allergyId}/history", patientId, allergyId)
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_ALLERGY_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Lễ tân gọi PUT update dị ứng -> 403 Forbidden")
    void updateAllergy_receptionist_shouldReturnForbidden() throws Exception {
        mockMvc.perform(put("/patients/{patientId}/allergies/{allergyId}", patientId, allergyId)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allergenName\":\"Aspirin\",\"severity\":\"MILD\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Lễ tân gọi DELETE dị ứng -> 403 Forbidden")
    void deleteAllergy_receptionist_shouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/patients/{patientId}/allergies/{allergyId}", patientId, allergyId)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cập nhật mục dị ứng không tồn tại -> 404 Not Found")
    void updateNonExistentAllergy_shouldReturnNotFound() throws Exception {
        when(updatePatientAllergyUseCase.updateAllergy(any()))
                .thenThrow(new PatientAllergyNotFoundException(allergyId));

        String payload = """
                {
                    "allergenName": "Penicillin",
                    "severity": "MILD"
                }
                """;

        mockMvc.perform(put("/patients/{patientId}/allergies/{allergyId}", patientId, allergyId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_ALLERGY_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PATIENT_ALLERGY_NOT_FOUND"));
    }
}
