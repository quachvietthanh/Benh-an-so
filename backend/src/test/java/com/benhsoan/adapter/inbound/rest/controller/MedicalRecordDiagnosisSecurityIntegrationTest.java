package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAuthorizationAuditService;
import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAuthorizationService;
import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordDiagnosisResultMapper;
import com.benhsoan.application.ucservice.medicalrecord.ReplaceMedicalRecordDiagnosesService;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ArchiveMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.DeleteMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordAccessLogsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordVersionHistoryUseCase;
import com.benhsoan.port.inbound.medicalrecord.IssueMedicalRecordCopyUseCase;
import com.benhsoan.port.inbound.medicalrecord.LockMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.SignMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicalRecordController.class)
@Import({
        AopAutoConfiguration.class,
        MedicalRecordDiagnosisSecurityIntegrationTest.AspectTestConfig.class,
        MedicalRecordRestMapper.class,
        MedicalRecordDetailRestMapper.class,
        MedicalRecordDiagnosisRestMapper.class,
        MedicalRecordDiagnosisResultMapper.class,
        ReplaceMedicalRecordDiagnosesService.class,
        MedicalRecordAuthorizationService.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class MedicalRecordDiagnosisSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateMedicalRecordUseCase createMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordUseCase getMedicalRecordUseCase;
    @MockitoBean private UpdateMedicalRecordUseCase updateMedicalRecordUseCase;
    @MockitoBean private LockMedicalRecordUseCase lockMedicalRecordUseCase;
    @MockitoBean private SignMedicalRecordUseCase signMedicalRecordUseCase;
    @MockitoBean private AmendMedicalRecordUseCase amendMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordAccessLogsUseCase getMedicalRecordAccessLogsUseCase;
    @MockitoBean private GetMedicalRecordDiagnosesUseCase getMedicalRecordDiagnosesUseCase;
    @MockitoBean private ArchiveMedicalRecordUseCase archiveMedicalRecordUseCase;
    @MockitoBean private DeleteMedicalRecordUseCase deleteMedicalRecordUseCase;
    @MockitoBean private IssueMedicalRecordCopyUseCase issueMedicalRecordCopyUseCase;
    @MockitoBean private GetMedicalRecordVersionHistoryUseCase getMedicalRecordVersionHistoryUseCase;
    @MockitoBean private MedicalRecordRepository medicalRecordRepository;
    @MockitoBean private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @MockitoBean private DiagnosisCatalogRepository diagnosisCatalogRepository;
    @MockitoBean private VisitRepository visitRepository;
    @MockitoBean private MedicalRecordAccessAuditService accessAuditService;
    @MockitoBean private MedicalRecordAuthorizationAuditService authorizationAuditService;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void doctorWithMedicalRecordUpdatePermissionCanReplaceDiagnoses() throws Exception {
        UUID doctorId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        MedicalRecord record = MedicalRecord.create(visitId, null, null, null, null, null, null, null, null, doctorId, NOW);
        Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), doctorId, null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, NOW, NOW, null, "Exam", null, doctorId, NOW, null);
        DiagnosisCatalog catalog = DiagnosisCatalog.restore(catalogId, "J06.9", "Upper respiratory infection",
                "Respiratory", null, true, NOW, null);

        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorId);
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(diagnosisCatalogRepository.findById(catalogId)).thenReturn(Optional.of(catalog));
        when(clockPort.now()).thenReturn(NOW);
        when(medicalRecordDiagnosisRepository.replaceForMedicalRecord(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        mockMvc.perform(replaceDiagnoses(recordId, "doctor", "ROLE_DOCTOR", catalogId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosisCode").value("J06.9"))
                .andExpect(jsonPath("$[0].diagnosisName").value("Upper respiratory infection"));
    }

    @Test
    void nurseReceptionistAndPharmacistAreForbiddenEvenWithMedicalRecordUpdatePermission() throws Exception {
        UUID recordId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        for (String role : List.of("NURSE", "RECEPTIONIST", "PHARMACIST")) {
            when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);

            mockMvc.perform(replaceDiagnoses(recordId, role.toLowerCase(), "ROLE_" + role, catalogId))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminDiagnosisWriteIsForbiddenAndAudited() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);

        mockMvc.perform(replaceDiagnoses(recordId, "admin", "ROLE_ADMIN", catalogId))
                .andExpect(status().isForbidden());

        verify(authorizationAuditService).recordDiagnosisWriteDenied(actorId, recordId);
        verify(medicalRecordRepository, never()).findById(any());
    }

    @Test
    void doctorCannotReplaceDiagnosesForAnotherDoctorsVisit() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID assignedDoctorId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        MedicalRecord record = MedicalRecord.create(visitId, null, null, null, null, null, null, null, null, assignedDoctorId, NOW);
        Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), assignedDoctorId, null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, NOW, NOW, null, "Exam", null, assignedDoctorId, NOW, null);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        mockMvc.perform(replaceDiagnoses(recordId, "doctor", "ROLE_DOCTOR", catalogId))
                .andExpect(status().isForbidden());

        verify(authorizationAuditService).recordDiagnosisWriteDenied(actorId, record.getId());
        verify(medicalRecordDiagnosisRepository, never()).replaceForMedicalRecord(any(), any());
        verify(accessAuditService, never()).recordRecordAccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void doctorGetsBadRequestWithoutWritingWhenPrimaryCatalogDoesNotExistOrIsInactive() throws Exception {
        UUID doctorId = UUID.randomUUID();
        UUID missingCatalogRecordId = UUID.randomUUID();
        UUID inactiveCatalogRecordId = UUID.randomUUID();
        UUID missingCatalogId = UUID.randomUUID();
        UUID inactiveCatalogId = UUID.randomUUID();
        UUID missingCatalogVisitId = UUID.randomUUID();
        UUID inactiveCatalogVisitId = UUID.randomUUID();
        MedicalRecord missingCatalogRecord = MedicalRecord.create(missingCatalogVisitId, null, null, null, null, null, null, null, null, doctorId, NOW);
        MedicalRecord inactiveCatalogRecord = MedicalRecord.create(inactiveCatalogVisitId, null, null, null, null, null, null, null, null, doctorId, NOW);
        Visit missingCatalogVisit = Visit.restore(missingCatalogVisitId, "VIS-001", UUID.randomUUID(), doctorId, null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, NOW, NOW, null, "Exam", null, doctorId, NOW, null);
        Visit inactiveCatalogVisit = Visit.restore(inactiveCatalogVisitId, "VIS-002", UUID.randomUUID(), doctorId, null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, NOW, NOW, null, "Exam", null, doctorId, NOW, null);
        DiagnosisCatalog inactiveCatalog = DiagnosisCatalog.restore(inactiveCatalogId, "J06.9", "Upper respiratory infection",
                "Respiratory", null, false, NOW, null);

        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorId);
        when(medicalRecordRepository.findById(missingCatalogRecordId)).thenReturn(Optional.of(missingCatalogRecord));
        when(medicalRecordRepository.findById(inactiveCatalogRecordId)).thenReturn(Optional.of(inactiveCatalogRecord));
        when(visitRepository.findById(missingCatalogVisitId)).thenReturn(Optional.of(missingCatalogVisit));
        when(visitRepository.findById(inactiveCatalogVisitId)).thenReturn(Optional.of(inactiveCatalogVisit));
        when(diagnosisCatalogRepository.findById(inactiveCatalogId)).thenReturn(Optional.of(inactiveCatalog));
        when(clockPort.now()).thenReturn(NOW);

        mockMvc.perform(replaceDiagnoses(missingCatalogRecordId, "doctor", "ROLE_DOCTOR", missingCatalogId))
                .andExpect(status().isBadRequest());
        mockMvc.perform(replaceDiagnoses(inactiveCatalogRecordId, "doctor", "ROLE_DOCTOR", inactiveCatalogId))
                .andExpect(status().isBadRequest());

        verify(medicalRecordDiagnosisRepository, never()).replaceForMedicalRecord(any(), any());
        verify(accessAuditService, never()).recordRecordAccess(any(), any(), any(), any(), any(), any(), any());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder replaceDiagnoses(
            UUID recordId,
            String username,
            String roleAuthority,
            UUID catalogId
    ) {
        return put("/medical-records/{medicalRecordId}/diagnoses", recordId)
                .with(user(username).authorities(
                        new SimpleGrantedAuthority(roleAuthority),
                        new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_UPDATE")
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"primaryDiagnosis":{"diagnosisCatalogId":"%s","code":"CLIENT_CODE","name":"Client diagnosis name"},"secondaryDiagnoses":[]}
                        """.formatted(catalogId));
    }
}
