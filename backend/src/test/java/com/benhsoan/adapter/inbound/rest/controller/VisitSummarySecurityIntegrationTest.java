package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.VisitRestMapper;
import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotSignedException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.VisitSummaryPrintResult;
import com.benhsoan.port.dto.result.VisitSummaryResult;
import com.benhsoan.port.inbound.visit.ExportVisitSummaryUseCase;
import com.benhsoan.port.inbound.visit.GetVisitEncounterUseCase;
import com.benhsoan.port.inbound.visit.GetVisitSummaryUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = VisitController.class)
@Import({
        AnonymizationModeState.class,
        AopAutoConfiguration.class,
        VisitSummarySecurityIntegrationTest.AspectTestConfig.class,
        VisitRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class VisitSummarySecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetVisitEncounterUseCase getVisitEncounterUseCase;
    @MockitoBean private GetVisitSummaryUseCase getVisitSummaryUseCase;
    @MockitoBean private ExportVisitSummaryUseCase exportVisitSummaryUseCase;
    @MockitoBean private com.benhsoan.port.inbound.visit.HandoverPatientUseCase handoverPatientUseCase;
    @MockitoBean private com.benhsoan.port.inbound.visit.GetVisitHandoversUseCase getVisitHandoversUseCase;
    @MockitoBean private com.benhsoan.port.inbound.user.GetDoctorsUseCase getDoctorsUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void userWithPermission_canGetVisitSummary() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(getVisitSummaryUseCase.getSummary(visitId)).thenReturn(emptySummary(visitId));

        mockMvc.perform(get("/visits/{visitId}/summary", visitId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_VISIT_SUMMARY_PRINT"))))
                .andExpect(status().isOk());
    }

    @Test
    void userWithPermission_canPrintVisitSummary() throws Exception {
        UUID visitId = UUID.randomUUID();
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(exportVisitSummaryUseCase.export(visitId)).thenReturn(
                new VisitSummaryPrintResult("phieu-tom-tat-VIS01.pdf", "application/pdf", pdfBytes)
        );

        mockMvc.perform(get("/visits/{visitId}/summary/print", visitId)
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_VISIT_SUMMARY_PRINT"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"phieu-tom-tat-VIS01.pdf\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes));
    }

    @Test
    void userWithoutPermission_isForbidden() throws Exception {
        UUID visitId = UUID.randomUUID();

        mockMvc.perform(get("/visits/{visitId}/summary", visitId)
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_READ"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/visits/{visitId}/summary/print", visitId)
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_PHARMACY_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_isUnauthorized() throws Exception {
        UUID visitId = UUID.randomUUID();

        mockMvc.perform(get("/visits/{visitId}/summary", visitId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/visits/{visitId}/summary/print", visitId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenMedicalRecordNotSigned_returnsBadRequest() throws Exception {
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        when(exportVisitSummaryUseCase.export(visitId))
                .thenThrow(new MedicalRecordNotSignedException(recordId, "Bệnh án chưa được ký"));

        mockMvc.perform(get("/visits/{visitId}/summary/print", visitId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_VISIT_SUMMARY_PRINT"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenDoctorAccessesUnauthorizedVisit_returnsForbidden() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(getVisitSummaryUseCase.getSummary(visitId))
                .thenThrow(new com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException("Bác sĩ chỉ có quyền xem và in phiếu tóm tắt của lượt khám do mình phụ trách."));
        when(exportVisitSummaryUseCase.export(visitId))
                .thenThrow(new com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException("Bác sĩ chỉ có quyền xem và in phiếu tóm tắt của lượt khám do mình phụ trách."));

        mockMvc.perform(get("/visits/{visitId}/summary", visitId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_VISIT_SUMMARY_PRINT"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/visits/{visitId}/summary/print", visitId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_VISIT_SUMMARY_PRINT"))))
                .andExpect(status().isForbidden());
    }

    private VisitSummaryResult emptySummary(UUID visitId) {
        return new VisitSummaryResult(
                visitId, "VIS01", java.time.Instant.now(),
                null, null, null, null,
                java.util.List.of(), java.util.List.of(),
                null, null, null, java.util.List.of()
        );
    }
}
