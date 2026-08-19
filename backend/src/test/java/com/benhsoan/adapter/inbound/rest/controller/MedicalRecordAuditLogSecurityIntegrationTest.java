package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDetailRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDiagnosisRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.MedicalRecordAccessLogResult;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordAccessLogsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.LockMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ReplaceMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicalRecordController.class)
@Import({
        AopAutoConfiguration.class,
        MedicalRecordAuditLogSecurityIntegrationTest.AspectTestConfig.class,
        MedicalRecordRestMapper.class,
        MedicalRecordDetailRestMapper.class,
        MedicalRecordDiagnosisRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class MedicalRecordAuditLogSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CreateMedicalRecordUseCase createMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordUseCase getMedicalRecordUseCase;
    @MockitoBean private UpdateMedicalRecordUseCase updateMedicalRecordUseCase;
    @MockitoBean private LockMedicalRecordUseCase lockMedicalRecordUseCase;
    @MockitoBean private AmendMedicalRecordUseCase amendMedicalRecordUseCase;
    @MockitoBean private GetMedicalRecordAccessLogsUseCase getMedicalRecordAccessLogsUseCase;
    @MockitoBean private GetMedicalRecordDiagnosesUseCase getMedicalRecordDiagnosesUseCase;
    @MockitoBean private ReplaceMedicalRecordDiagnosesUseCase replaceMedicalRecordDiagnosesUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    private final UUID patientId = UUID.randomUUID();
    private final UUID visitId = UUID.randomUUID();
    private final UUID medicalRecordId = UUID.randomUUID();
    private final UUID accessedBy = UUID.randomUUID();

    @Test
    void forbidsDoctorWithoutAuditReadPermission() throws Exception {
        mockMvc.perform(get("/medical-records/access-logs")
                        .param("patientId", patientId.toString())
                        .with(user("doctor").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getMedicalRecordAccessLogsUseCase);
    }

    @Test
    void allowsAdminWithAuditReadPermissionAndReturns200() throws Exception {
        when(getMedicalRecordAccessLogsUseCase.getAccessLogs(any())).thenReturn(new PageImpl<>(
                List.of(new MedicalRecordAccessLogResult(
                        UUID.randomUUID(),
                        patientId,
                        visitId,
                        medicalRecordId,
                        accessedBy,
                        MedicalRecordAccessAction.VIEW,
                        "Medical record viewed",
                        Instant.parse("2026-08-12T01:00:00Z")
                )),
                PageRequest.of(0, 20),
                1
        ));

        mockMvc.perform(get("/medical-records/access-logs")
                        .param("patientId", patientId.toString())
                        .param("from", "2026-08-01T00:00:00Z")
                        .param("to", "2026-08-12T23:59:59Z")
                        .param("page", "0")
                        .param("size", "20")
                        .with(user("admin").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_AUDIT_READ"))))
                .andExpect(status().isOk());
    }
}
