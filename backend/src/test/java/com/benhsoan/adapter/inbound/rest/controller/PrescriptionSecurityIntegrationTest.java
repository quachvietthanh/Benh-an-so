package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PrescriptionRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.inbound.prescription.AmendPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.CancelPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.inbound.prescription.CreatePrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.DispensePrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.ExportPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionsByMedicalRecordUseCase;
import com.benhsoan.port.inbound.prescription.SearchPrescriptionsUseCase;
import com.benhsoan.port.inbound.prescription.SendPrescriptionInterconnectionUseCase;
import com.benhsoan.port.inbound.prescription.RetryPrescriptionInterconnectionUseCase;
import com.benhsoan.port.dto.result.PrescriptionInterconnectionResult;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = {PrescriptionController.class, PrescriptionInterconnectionController.class})
@Import({PrescriptionRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class,
        RequirePermissionAspect.class, PermissionEvaluator.class, PrescriptionSecurityIntegrationTest.AspectTestConfig.class})
class PrescriptionSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreatePrescriptionUseCase createPrescriptionUseCase;
    @MockitoBean private AmendPrescriptionUseCase amendPrescriptionUseCase;
    @MockitoBean private GetPrescriptionUseCase getPrescriptionUseCase;
    @MockitoBean private GetPrescriptionsByMedicalRecordUseCase getPrescriptionsByMedicalRecordUseCase;
    @MockitoBean private SearchPrescriptionsUseCase searchPrescriptionsUseCase;
    @MockitoBean private DispensePrescriptionUseCase dispensePrescriptionUseCase;
    @MockitoBean private CancelPrescriptionUseCase cancelPrescriptionUseCase;
    @MockitoBean private CheckDrugInteractionUseCase checkDrugInteractionUseCase;
    @MockitoBean private ExportPrescriptionUseCase exportPrescriptionUseCase;
    @MockitoBean private SendPrescriptionInterconnectionUseCase sendPrescriptionInterconnectionUseCase;
    @MockitoBean private RetryPrescriptionInterconnectionUseCase retryPrescriptionInterconnectionUseCase;
    @MockitoBean private com.benhsoan.port.inbound.prescription.SearchPrescriptionInterconnectionsUseCase searchPrescriptionInterconnectionsUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void onlyAllowsPharmacistsAndAdminsToReadDispensingQueue() throws Exception {
        when(searchPrescriptionsUseCase.search(any())).thenReturn(Page.empty());

        for (String role : new String[] {"ADMIN", "PHARMACIST"}) {
            mockMvc.perform(get("/prescriptions")
                            .param("status", "PENDING_DISPENSE")
                            .with(user("tester").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_READ"))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/prescriptions")
                        .param("status", "PENDING_DISPENSE")
                        .with(user("doctor").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_UPDATE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminsAndDoctorsToCheckInteractions() throws Exception {
        when(checkDrugInteractionUseCase.check(any())).thenReturn(java.util.List.of());

        String body = """
                {
                  "drugIds": [
                    "16000000-0000-0000-0000-000000000001",
                    "16000000-0000-0000-0000-000000000002"
                  ]
                }
                """;

        for (String role : new String[] {"ADMIN", "DOCTOR"}) {
            mockMvc.perform(post("/prescriptions/check-interactions")
                            .with(user("tester").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_CREATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/prescriptions/check-interactions")
                        .with(user("pharmacist").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsDoctorsAndPharmacistsButRejectsReceptionistsToPrint() throws Exception {
        java.util.UUID prescriptionId = java.util.UUID.randomUUID();
        when(exportPrescriptionUseCase.export(prescriptionId))
                .thenReturn(new com.benhsoan.port.dto.result.PrescriptionPrintResult(
                        "prescription-RX-001.pdf", "application/pdf", "%PDF".getBytes()));

        for (String userRole : new String[] {"DOCTOR", "PHARMACIST"}) {
            mockMvc.perform(get("/prescriptions/{id}/print", prescriptionId)
                            .with(user(userRole.toLowerCase())
                                    .authorities(
                                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                    "ROLE_" + userRole),
                                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                    "PERMISSION_PRESCRIPTION_PRINT"))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/prescriptions/{id}/print", prescriptionId)
                        .with(user("receptionist").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "ROLE_RECEPTIONIST"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "PERMISSION_PRESCRIPTION_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsInternalServerErrorWhenPdfRenderingFails() throws Exception {
        java.util.UUID prescriptionId = java.util.UUID.randomUUID();
        when(exportPrescriptionUseCase.export(prescriptionId)).thenThrow(
                new com.benhsoan.infrastructure.pdf.PdfRenderingException(
                        "Unable to generate prescription PDF.", new java.io.IOException("Render failure")
                )
        );

        mockMvc.perform(get("/prescriptions/{id}/print", prescriptionId)
                        .with(user("doctor").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_DOCTOR"), new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "PERMISSION_PRESCRIPTION_PRINT"))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void returnsConflictWithVietnameseMessageWhenPrescriptionIsNotComplete() throws Exception {
        java.util.UUID prescriptionId = java.util.UUID.randomUUID();
        when(exportPrescriptionUseCase.export(prescriptionId)).thenThrow(
                new com.benhsoan.domain.prescription.exception.PrescriptionNotPrintableException("Đơn chưa hoàn tất")
        );

        mockMvc.perform(get("/prescriptions/{id}/print", prescriptionId)
                        .with(user("doctor").authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_DOCTOR"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "PERMISSION_PRESCRIPTION_PRINT"))))
                .andExpect(status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Đơn chưa hoàn tất"));
    }

    @Test
    void requiresDedicatedInterconnectionPermissions() throws Exception {
        java.util.UUID prescriptionId = java.util.UUID.randomUUID();
        var result = new PrescriptionInterconnectionResult(
                prescriptionId, "RX000001", InterconnectionStatus.SUCCESS,
                "LT-20260821-000001", null, java.time.Instant.parse("2026-08-21T03:00:00Z"));
        when(sendPrescriptionInterconnectionUseCase.send(prescriptionId)).thenReturn(result);
        when(retryPrescriptionInterconnectionUseCase.retry(prescriptionId)).thenReturn(result);

        mockMvc.perform(post("/prescriptions/{id}/interconnection", prescriptionId)
                        .with(user("doctor").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "PERMISSION_PRESCRIPTION_INTERCONNECTION_SEND"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/prescriptions/{id}/interconnection", prescriptionId)
                        .with(user("doctor").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "PERMISSION_PRESCRIPTION_UPDATE"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/prescriptions/{id}/interconnection/retry", prescriptionId)
                        .with(user("admin").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "PERMISSION_PRESCRIPTION_INTERCONNECTION_RETRY"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/prescriptions/{id}/interconnection/retry", prescriptionId)
                        .with(user("pharmacist").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "PERMISSION_PRESCRIPTION_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void pharmacistIsForbiddenFromInterconnectionSearchAndRetry() throws Exception {
        java.util.UUID prescriptionId = java.util.UUID.randomUUID();
        var pharmacist = user("pharmacist").authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_READ"));

        mockMvc.perform(get("/prescription-interconnections").param("status", "FAILED").with(pharmacist))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/prescriptions/{id}/interconnection/retry", prescriptionId).with(pharmacist))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsConflictWhenSuccessfullyInterconnectedPrescriptionIsSentAgain() throws Exception {
        java.util.UUID prescriptionId = java.util.UUID.randomUUID();
        when(sendPrescriptionInterconnectionUseCase.send(prescriptionId)).thenThrow(
                new com.benhsoan.domain.prescription.exception.PrescriptionInvalidStatusException(
                        "Successfully interconnected prescriptions cannot be submitted again."));

        mockMvc.perform(post("/prescriptions/{id}/interconnection", prescriptionId)
                        .with(user("doctor").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "PERMISSION_PRESCRIPTION_INTERCONNECTION_SEND"))))
                .andExpect(status().isConflict());
    }
}
