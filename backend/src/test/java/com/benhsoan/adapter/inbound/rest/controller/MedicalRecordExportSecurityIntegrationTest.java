package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDetailRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDiagnosisRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.OverdueMedicalRecordRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.CurrentUserAdapter;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.MedicalRecordExchangeExportResult;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ApplyMedicalRecordTemplateUseCase;
import com.benhsoan.port.inbound.medicalrecord.ArchiveMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.DeleteMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ExportMedicalRecordExchangeUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordAccessLogsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordTemplateSelectionUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordVersionHistoryUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetOverdueMedicalRecordsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetSigningRemindersUseCase;
import com.benhsoan.port.inbound.medicalrecord.IssueMedicalRecordCopyUseCase;
import com.benhsoan.port.inbound.medicalrecord.LockMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ReplaceMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.SendSigningReminderUseCase;
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
        MedicalRecordExportSecurityIntegrationTest.AspectTestConfig.class,
        MedicalRecordRestMapper.class,
        MedicalRecordDetailRestMapper.class,
        MedicalRecordDiagnosisRestMapper.class,
        OverdueMedicalRecordRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        CurrentUserAdapter.class
})
class MedicalRecordExportSecurityIntegrationTest {

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
    @MockitoBean private ExportMedicalRecordExchangeUseCase exportMedicalRecordExchangeUseCase;
    @MockitoBean private GetMedicalRecordVersionHistoryUseCase getMedicalRecordVersionHistoryUseCase;
    @MockitoBean private GetOverdueMedicalRecordsUseCase getOverdueMedicalRecordsUseCase;
    @MockitoBean private SendSigningReminderUseCase sendSigningReminderUseCase;
    @MockitoBean private GetSigningRemindersUseCase getSigningRemindersUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    private final UUID recordId = UUID.randomUUID();

    @Test
    @DisplayName("F-07 & F-08: Xuất đơn lẻ thành công khi có quyền PERMISSION_MEDICAL_RECORD_EXPORT")
    void exportSingleRecord_Returns200_WhenHasExportPermission() throws Exception {
        byte[] payload = "{\"version\":\"1.0\"}".getBytes(StandardCharsets.UTF_8);
        when(exportMedicalRecordExchangeUseCase.exportSingleRecord(recordId))
                .thenReturn(new MedicalRecordExchangeExportResult("emr-data-test.json", "application/json", payload, 1));

        mockMvc.perform(get("/medical-records/{id}/export", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.user("officer")
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_EXPORT"))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("emr-data-test.json")));
    }

    @Test
    @DisplayName("F-07: Xuất đơn lẻ thành công khi có vai trò ROLE_ADMIN và quyền export")
    void exportSingleRecord_Returns200_WhenRoleAdmin() throws Exception {
        byte[] payload = "{\"version\":\"1.0\"}".getBytes(StandardCharsets.UTF_8);
        when(exportMedicalRecordExchangeUseCase.exportSingleRecord(recordId))
                .thenReturn(new MedicalRecordExchangeExportResult("emr-data-test.json", "application/json", payload, 1));

        mockMvc.perform(get("/medical-records/{id}/export", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.user("admin")
                                .authorities(
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_EXPORT"),
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")
                                )))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("F-07: Xuất đơn lẻ thành công khi có vai trò ROLE_MANAGER và quyền export")
    void exportSingleRecord_Returns200_WhenRoleManager() throws Exception {
        byte[] payload = "{\"version\":\"1.0\"}".getBytes(StandardCharsets.UTF_8);
        when(exportMedicalRecordExchangeUseCase.exportSingleRecord(recordId))
                .thenReturn(new MedicalRecordExchangeExportResult("emr-data-test.json", "application/json", payload, 1));

        mockMvc.perform(get("/medical-records/{id}/export", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.user("manager")
                                .authorities(
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_EXPORT"),
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MANAGER")
                                )))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: Từ chối xuất 403 Forbidden khi bác sĩ ROLE_DOCTOR không có quyền export")
    void exportSingleRecord_Returns403_WhenRoleDoctor() throws Exception {
        mockMvc.perform(get("/medical-records/{id}/export", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.user("doctor")
                                .roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Từ chối xuất 401 Unauthorized khi chưa đăng nhập")
    void exportSingleRecord_Returns401_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/medical-records/{id}/export", recordId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("F-07: Xuất hàng loạt (batch) thành công khi có quyền PERMISSION_MEDICAL_RECORD_EXPORT")
    void exportBatchRecords_Returns200_WhenHasExportPermission() throws Exception {
        byte[] payload = "{\"version\":\"1.0\"}".getBytes(StandardCharsets.UTF_8);
        when(exportMedicalRecordExchangeUseCase.exportRecords(any()))
                .thenReturn(new MedicalRecordExchangeExportResult("emr-data-batch.json", "application/json", payload, 1));

        String requestJson = """
                {
                    "medicalRecordIds": ["%s"]
                }
                """.formatted(recordId);

        mockMvc.perform(post("/medical-records/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .with(SecurityMockMvcRequestPostProcessors.user("officer")
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_EXPORT"))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("emr-data-batch.json")));
    }

    @Test
    @DisplayName("Security: Từ chối xuất hàng loạt 403 Forbidden khi dược sĩ ROLE_PHARMACIST không có quyền")
    void exportBatchRecords_Returns403_WhenRolePharmacist() throws Exception {
        String requestJson = """
                {
                    "medicalRecordIds": ["%s"]
                }
                """.formatted(recordId);

        mockMvc.perform(post("/medical-records/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .with(SecurityMockMvcRequestPostProcessors.user("pharmacist")
                                .roles("PHARMACIST")))
                .andExpect(status().isForbidden());
    }
}
