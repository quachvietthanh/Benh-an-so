package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.patient.enums.ImportStatus;
import com.benhsoan.domain.patient.exception.PatientImportLogNotFoundException;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.patient.PatientImportLogResult;
import com.benhsoan.port.dto.result.patient.PatientImportPreviewResult;
import com.benhsoan.port.dto.result.patient.PatientImportResult;
import com.benhsoan.port.inbound.patient.DownloadPatientImportTemplateUseCase;
import com.benhsoan.port.inbound.patient.FindDuplicatePatientsUseCase;
import com.benhsoan.port.inbound.patient.GetPatientByCodeUseCase;
import com.benhsoan.port.inbound.patient.GetPatientByIdUseCase;
import com.benhsoan.port.inbound.patient.GetPatientImportLogsUseCase;
import com.benhsoan.port.inbound.patient.ImportPatientsUseCase;
import com.benhsoan.port.inbound.patient.MergePatientsUseCase;
import com.benhsoan.port.inbound.patient.PreviewPatientImportUseCase;
import com.benhsoan.port.inbound.patient.RegisterPatientUseCase;
import com.benhsoan.port.inbound.patient.SearchPatientUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientPregnancyStatusUseCase;
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
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        PatientImportControllerIntegrationTest.AspectTestConfig.class
})
@DisplayName("Patient Import Controller & Security Integration Tests (NCL-02-CN-010)")
class PatientImportControllerIntegrationTest {

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
    @MockitoBean private DownloadPatientImportTemplateUseCase downloadPatientImportTemplateUseCase;
    @MockitoBean private PreviewPatientImportUseCase previewPatientImportUseCase;
    @MockitoBean private ImportPatientsUseCase importPatientsUseCase;
    @MockitoBean private GetPatientImportLogsUseCase getPatientImportLogsUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    private static final SimpleGrantedAuthority PERMISSION_IMPORT = new SimpleGrantedAuthority("PERMISSION_PATIENT_IMPORT");
    private static final SimpleGrantedAuthority PERMISSION_PATIENT_READ = new SimpleGrantedAuthority("PERMISSION_PATIENT_READ");

    @Test
    @DisplayName("Allows Admin/Receptionist with PATIENT_IMPORT to download template")
    void allowsAuthorizedUserToDownloadTemplate() throws Exception {
        byte[] fakeTemplate = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(downloadPatientImportTemplateUseCase.downloadTemplate()).thenReturn(fakeTemplate);

        mockMvc.perform(get("/patients/import/template")
                        .with(user("admin").authorities(PERMISSION_IMPORT)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau_danh_sach_benh_nhan.xlsx\""));
    }

    @Test
    @DisplayName("Blocks user without PATIENT_IMPORT permission from downloading template (403)")
    void blocksUnauthorizedUserFromDownloadTemplate() throws Exception {
        mockMvc.perform(get("/patients/import/template")
                        .with(user("doctor").authorities(PERMISSION_PATIENT_READ)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Allows preview with valid xlsx file upload")
    void allowsPreviewWithValidXlsx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "danh_sach.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        when(previewPatientImportUseCase.preview(any())).thenReturn(
                new PatientImportPreviewResult("danh_sach.xlsx", 10, 8, 2, 0, List.of(), List.of())
        );

        mockMvc.perform(multipart("/patients/import/preview")
                        .file(file)
                        .with(user("admin").authorities(PERMISSION_IMPORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(10))
                .andExpect(jsonPath("$.validCount").value(8))
                .andExpect(jsonPath("$.errorCount").value(2));
    }

    @Test
    @DisplayName("Rejects preview when file format is not .xlsx or .xls (400)")
    void rejectsInvalidFileExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "invalid content".getBytes()
        );

        mockMvc.perform(multipart("/patients/import/preview")
                        .file(file)
                        .with(user("admin").authorities(PERMISSION_IMPORT)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Allows import with valid xlsx and skipDuplicates")
    void allowsImportWithValidXlsx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "danh_sach.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        UUID logId = UUID.randomUUID();
        when(importPatientsUseCase.importPatients(any())).thenReturn(
                new PatientImportResult(logId, "danh_sach.xlsx", 5, 5, 0, 0, List.of("BN-001"), List.of())
        );

        mockMvc.perform(multipart("/patients/import")
                        .file(file)
                        .param("skipDuplicates", "true")
                        .with(user("admin").authorities(PERMISSION_IMPORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importLogId").value(logId.toString()))
                .andExpect(jsonPath("$.successCount").value(5));
    }

    @Test
    @DisplayName("Allows getting import logs with pagination")
    void allowsGettingImportLogs() throws Exception {
        UUID logId = UUID.randomUUID();
        PatientImportLogResult logResult = new PatientImportLogResult(
                logId, "test.xlsx", 1000, 5, 5, 0, 0,
                ImportStatus.SUCCESS, UUID.randomUUID(), "Admin", Instant.now(), List.of()
        );

        when(getPatientImportLogsUseCase.getLogs(any())).thenReturn(
                new PageImpl<>(List.of(logResult), PageRequest.of(0, 20), 1)
        );

        mockMvc.perform(get("/patients/import-logs")
                        .with(user("admin").authorities(PERMISSION_IMPORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(logId.toString()));
    }

    @Test
    @DisplayName("Returns 404 when import log is not found")
    void returns404WhenLogNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(getPatientImportLogsUseCase.getLogById(missingId))
                .thenThrow(new PatientImportLogNotFoundException(missingId));

        mockMvc.perform(get("/patients/import-logs/" + missingId)
                        .with(user("admin").authorities(PERMISSION_IMPORT)))
                .andExpect(status().isNotFound());
    }
}
