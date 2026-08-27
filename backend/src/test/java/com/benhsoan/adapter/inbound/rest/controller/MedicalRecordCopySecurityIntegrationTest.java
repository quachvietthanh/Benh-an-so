package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotSignedException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.MedicalRecordCopyResult;
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
import com.benhsoan.port.inbound.medicalrecord.ReplaceMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.SignMedicalRecordUseCase;
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
        AopAutoConfiguration.class,
        MedicalRecordCopySecurityIntegrationTest.AspectTestConfig.class,
        MedicalRecordRestMapper.class,
        MedicalRecordDetailRestMapper.class,
        MedicalRecordDiagnosisRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class MedicalRecordCopySecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CreateMedicalRecordUseCase createMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordUseCase getMedicalRecordUseCase;
    @MockitoBean private com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordTemplateSelectionUseCase getMedicalRecordTemplateSelectionUseCase;
    @MockitoBean private com.benhsoan.port.inbound.medicalrecord.ApplyMedicalRecordTemplateUseCase applyMedicalRecordTemplateUseCase;
    @MockitoBean private UpdateMedicalRecordUseCase updateMedicalRecordUseCase;
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
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    private final UUID recordId = UUID.randomUUID();

    @Test
    void managerWithCopyPermissionReturnsPdf() throws Exception {
        when(issueMedicalRecordCopyUseCase.issue(any()))
                .thenReturn(new MedicalRecordCopyResult("copy.pdf", "application/pdf", "%PDF-1.4".getBytes()));

        mockMvc.perform(post("/medical-records/{medicalRecordId}/copy", recordId)
                        .with(user("manager")
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_MANAGER"),
                                        new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_COPY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientType":"PATIENT","recipientName":"Nguyen Van A","recipientIdentityNumber":"012345678","requestReason":"Xin ban sao"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    void doctorWithoutCopyPermissionIsForbidden() throws Exception {
        mockMvc.perform(post("/medical-records/{medicalRecordId}/copy", recordId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientType":"PATIENT","recipientName":"Nguyen Van A","recipientIdentityNumber":"012345678"}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(issueMedicalRecordCopyUseCase);
    }

    @Test
    void managerReceivesBadRequestForUnsignedRecord() throws Exception {
        when(issueMedicalRecordCopyUseCase.issue(any()))
                .thenThrow(new MedicalRecordNotSignedException(recordId));

        mockMvc.perform(post("/medical-records/{medicalRecordId}/copy", recordId)
                        .with(user("manager")
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_MANAGER"),
                                        new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_COPY")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientType":"PATIENT","recipientName":"Nguyen Van A","recipientIdentityNumber":"012345678"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
