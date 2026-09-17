package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDetailRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDiagnosisRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.CurrentUserAdapter;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ApplyMedicalRecordTemplateUseCase;
import com.benhsoan.port.inbound.medicalrecord.ArchiveMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.DeleteMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordAccessLogsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordTemplateSelectionUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordVersionHistoryUseCase;
import com.benhsoan.port.inbound.medicalrecord.IssueMedicalRecordCopyUseCase;
import com.benhsoan.port.inbound.medicalrecord.LockMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ReplaceMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.SignMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateInstructionsAndTreatmentPlanUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicalRecordController.class)
@Import({
        AnonymizationModeState.class,
        AopAutoConfiguration.class,
        MedicalRecordInstructionsSecurityIntegrationTest.AspectTestConfig.class,
        MedicalRecordRestMapper.class,
        MedicalRecordDetailRestMapper.class,
        MedicalRecordDiagnosisRestMapper.class,
        com.benhsoan.adapter.inbound.rest.mapper.OverdueMedicalRecordRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        CurrentUserAdapter.class
})
@DisplayName("MedicalRecordInstructionsSecurityIntegrationTest - Security & RBAC (P2-2)")
class MedicalRecordInstructionsSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CreateMedicalRecordUseCase createMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordUseCase getMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordTemplateSelectionUseCase getMedicalRecordTemplateSelectionUseCase;
    @MockitoBean private ApplyMedicalRecordTemplateUseCase applyMedicalRecordTemplateUseCase;
    @MockitoBean private UpdateMedicalRecordUseCase updateMedicalRecordUseCase;
    @MockitoBean private UpdateInstructionsAndTreatmentPlanUseCase updateInstructionsAndTreatmentPlanUseCase;
    @MockitoBean private LockMedicalRecordUseCase lockMedicalRecordUseCase;
    @MockitoBean private SignMedicalRecordUseCase signMedicalRecordUseCase;
    @MockitoBean private AmendMedicalRecordUseCase amendMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordAccessLogsUseCase getMedicalRecordAccessLogsUseCase;
    @MockitoBean private GetMedicalRecordDiagnosesUseCase getMedicalRecordDiagnosesUseCase;
    @MockitoBean private ReplaceMedicalRecordDiagnosesUseCase replaceMedicalRecordDiagnosesUseCase;
    @MockitoBean private ArchiveMedicalRecordUseCase archiveMedicalRecordUseCase;
    @MockitoBean private DeleteMedicalRecordUseCase deleteMedicalRecordUseCase;
    @MockitoBean private IssueMedicalRecordCopyUseCase issueMedicalRecordCopyUseCase;
    @MockitoBean private GetMedicalRecordVersionHistoryUseCase getMedicalRecordVersionHistoryUseCase;
    @MockitoBean private com.benhsoan.port.inbound.medicalrecord.GetOverdueMedicalRecordsUseCase getOverdueMedicalRecordsUseCase;
    @MockitoBean private com.benhsoan.port.inbound.medicalrecord.SendSigningReminderUseCase sendSigningReminderUseCase;
    @MockitoBean private com.benhsoan.port.inbound.medicalrecord.GetSigningRemindersUseCase getSigningRemindersUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    private final UUID recordId = UUID.randomUUID();
    private final UUID visitId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-20T02:00:00Z");

    @Test
    @DisplayName("Bác sĩ có quyền MEDICAL_RECORD_UPDATE cập nhật thành công (200 OK)")
    void doctorWithMedicalRecordUpdatePermissionReturns200() throws Exception {
        LocalDate revisitDate = LocalDate.of(2026, 8, 27);
        MedicalRecordResult result = new MedicalRecordResult(
                recordId, visitId, "Headache", null, null, null, null,
                "Kế hoạch điều trị 7 ngày", "Uống nhiều nước, nghỉ ngơi", "Ổn định",
                revisitDate, MedicalRecordStatus.DRAFT, null, null, null, null, null,
                doctorId, now, doctorId, now, null
        );

        when(updateInstructionsAndTreatmentPlanUseCase.updateInstructionsAndTreatmentPlan(any(), any()))
                .thenReturn(result);

        mockMvc.perform(put("/medical-records/{medicalRecordId}/instructions-and-treatment-plan", recordId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_UPDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "treatmentPlan": "Kế hoạch điều trị 7 ngày",
                                    "doctorInstructions": "Uống nhiều nước, nghỉ ngơi",
                                    "revisitDate": "2026-08-27"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recordId.toString()))
                .andExpect(jsonPath("$.treatmentPlan").value("Kế hoạch điều trị 7 ngày"))
                .andExpect(jsonPath("$.doctorInstructions").value("Uống nhiều nước, nghỉ ngơi"))
                .andExpect(jsonPath("$.revisitDate").value("2026-08-27"));
    }

    @Test
    @DisplayName("Lễ tân không có quyền MEDICAL_RECORD_UPDATE bị chặn (403 Forbidden)")
    void receptionistWithoutMedicalRecordUpdatePermissionReturns403() throws Exception {
        mockMvc.perform(put("/medical-records/{medicalRecordId}/instructions-and-treatment-plan", recordId)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "treatmentPlan": "Kế hoạch",
                                    "doctorInstructions": "Lời dặn",
                                    "revisitDate": "2026-08-27"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Request chưa xác thực trả về (401 Unauthorized)")
    void unauthenticatedUserReturns401() throws Exception {
        mockMvc.perform(put("/medical-records/{medicalRecordId}/instructions-and-treatment-plan", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "treatmentPlan": "Kế hoạch",
                                    "doctorInstructions": "Lời dặn",
                                    "revisitDate": "2026-08-27"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bác sĩ không phụ trách lượt khám bị từ chối nghiệp vụ (403 Forbidden - MEDICAL_RECORD_ACCESS_DENIED)")
    void nonAttendingDoctorReturns403AccessDenied() throws Exception {
        when(updateInstructionsAndTreatmentPlanUseCase.updateInstructionsAndTreatmentPlan(any(), any()))
                .thenThrow(new MedicalRecordAccessDeniedException());

        mockMvc.perform(put("/medical-records/{medicalRecordId}/instructions-and-treatment-plan", recordId)
                        .with(user("other_doctor").authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_UPDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "treatmentPlan": "Kế hoạch",
                                    "doctorInstructions": "Lời dặn",
                                    "revisitDate": "2026-08-27"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEDICAL_RECORD_ACCESS_DENIED"));
    }
}
