package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.ArchiveMedicalRecordRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.CurrentUserAdapter;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.BatchArchiveMedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.BatchArchiveMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetEligibleForArchiveMedicalRecordsUseCase;
import com.benhsoan.port.inbound.medicalrecord.SearchArchivedMedicalRecordsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicalRecordArchiveController.class)
@Import({
        AnonymizationModeState.class,
        AopAutoConfiguration.class,
        MedicalRecordArchiveControllerTest.AspectTestConfig.class,
        ArchiveMedicalRecordRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        CurrentUserAdapter.class
})
class MedicalRecordArchiveControllerTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GetEligibleForArchiveMedicalRecordsUseCase getEligibleForArchiveMedicalRecordsUseCase;
    @MockitoBean private BatchArchiveMedicalRecordUseCase batchArchiveMedicalRecordUseCase;
    @MockitoBean private SearchArchivedMedicalRecordsUseCase searchArchivedMedicalRecordsUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    @DisplayName("Unauthenticated request to eligible records returns 401")
    void unauthenticatedEligibleReturns401() throws Exception {
        mockMvc.perform(get("/medical-records/archive/eligible"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to search archived records returns 401")
    void unauthenticatedSearchReturns401() throws Exception {
        mockMvc.perform(get("/medical-records/archive"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to batch archive returns 401")
    void unauthenticatedBatchArchiveReturns401() throws Exception {
        mockMvc.perform(post("/medical-records/archive/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicalRecordIds\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("User without MEDICAL_RECORD_ARCHIVE_MANAGE permission returns 403 on eligible")
    void userWithoutManagePermissionReturns403OnEligible() throws Exception {
        mockMvc.perform(get("/medical-records/archive/eligible")
                        .with(SecurityMockMvcRequestPostProcessors.user("doctor")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User without MEDICAL_RECORD_ARCHIVE_MANAGE permission returns 403 on batch")
    void userWithoutManagePermissionReturns403OnBatch() throws Exception {
        mockMvc.perform(post("/medical-records/archive/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicalRecordIds\":[]}")
                        .with(SecurityMockMvcRequestPostProcessors.user("doctor")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User with MEDICAL_RECORD_ARCHIVE_MANAGE permission returns 200 on eligible")
    void userWithManagePermissionReturns200OnEligible() throws Exception {
        when(getEligibleForArchiveMedicalRecordsUseCase.getEligibleRecords(any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/medical-records/archive/eligible")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_ARCHIVE_MANAGE"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User with MEDICAL_RECORD_ARCHIVE_MANAGE permission returns 200 on batch archive")
    void userWithManagePermissionReturns200OnBatch() throws Exception {
        when(batchArchiveMedicalRecordUseCase.batchArchive(any(), any(Boolean.class)))
                .thenReturn(BatchArchiveMedicalRecordResult.builder()
                        .totalRequested(0)
                        .archivedCount(0)
                        .skippedCount(0)
                        .archivedMedicalRecordIds(List.of())
                        .skippedMedicalRecordIds(List.of())
                        .build());

        mockMvc.perform(post("/medical-records/archive/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicalRecordIds\":[],\"archiveAllEligible\":false}")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_ARCHIVE_MANAGE"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User without MEDICAL_RECORD_ARCHIVE_READ permission returns 403 on search")
    void userWithoutReadPermissionReturns403OnSearch() throws Exception {
        mockMvc.perform(get("/medical-records/archive")
                        .with(SecurityMockMvcRequestPostProcessors.user("receptionist")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User with MEDICAL_RECORD_ARCHIVE_READ permission returns 200 on search")
    void userWithReadPermissionReturns200OnSearch() throws Exception {
        when(searchArchivedMedicalRecordsUseCase.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/medical-records/archive")
                        .with(SecurityMockMvcRequestPostProcessors.user("doctor")
                                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_ARCHIVE_READ"))))
                .andExpect(status().isOk());
    }
}
