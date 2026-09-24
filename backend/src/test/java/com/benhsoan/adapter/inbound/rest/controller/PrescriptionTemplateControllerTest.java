package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PrescriptionTemplateRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.result.AppliedPrescriptionTemplateResult;
import com.benhsoan.port.dto.result.PrescriptionTemplateResult;
import com.benhsoan.port.inbound.prescription.ApplyPrescriptionTemplateUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionTemplateUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionTemplatesUseCase;
import com.benhsoan.port.inbound.prescription.SavePrescriptionTemplateUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PrescriptionTemplateController.class)
@Import({
        PrescriptionTemplateRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class PrescriptionTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private SavePrescriptionTemplateUseCase savePrescriptionTemplateUseCase;
    @MockitoBean private ApplyPrescriptionTemplateUseCase applyPrescriptionTemplateUseCase;
    @MockitoBean private GetPrescriptionTemplatesUseCase getPrescriptionTemplatesUseCase;
    @MockitoBean private GetPrescriptionTemplateUseCase getPrescriptionTemplateUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void saveReturnsCreatedTemplate() throws Exception {
        when(savePrescriptionTemplateUseCase.save(any())).thenReturn(templateResult());

        mockMvc.perform(post("/prescription-templates")
                        .with(user("doctor").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prescriptionId\":\"" + UUID.randomUUID() + "\",\"diagnosisCode\":\"J06.9\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.diagnosisCode").value("J06.9"));
    }

    @Test
    void applyReturnsDraftWithSkippedItems() throws Exception {
        UUID templateId = UUID.randomUUID();
        when(applyPrescriptionTemplateUseCase.apply(any())).thenReturn(appliedResult());

        mockMvc.perform(post("/prescription-templates/{id}/apply", templateId)
                        .with(user("doctor").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicalRecordId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosisCode").value("J06.9"))
                .andExpect(jsonPath("$.items[0].medicineName").value("Paracetamol"))
                .andExpect(jsonPath("$.skippedItems[0].medicineName").value("Thuốc cũ"));
    }

    @Test
    void listReturnsOwnTemplates() throws Exception {
        when(getPrescriptionTemplatesUseCase.getByDiagnosisCode("J06.9"))
                .thenReturn(List.of(templateResult()));

        mockMvc.perform(get("/prescription-templates").param("diagnosisCode", "J06.9")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosisCode").value("J06.9"));
    }

    @Test
    void getByIdReturnsTemplate() throws Exception {
        UUID id = UUID.randomUUID();
        when(getPrescriptionTemplateUseCase.getById(id)).thenReturn(templateResult());

        mockMvc.perform(get("/prescription-templates/{id}", id)
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosisCode").value("J06.9"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/prescription-templates").param("diagnosisCode", "J06.9"))
                .andExpect(status().isUnauthorized());
    }

    private PrescriptionTemplateResult templateResult() {
        UUID medicineId = UUID.randomUUID();
        return new PrescriptionTemplateResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "J06.9",
                "Viêm họng",
                UUID.randomUUID(),
                Instant.parse("2026-09-30T01:00:00Z"),
                List.of(new PrescriptionTemplateResult.Item(
                        UUID.randomUUID(), medicineId, "MED001", "Paracetamol", "Paracetamol",
                        "500mg", "viên", "1 viên", 2, AdministrationRoute.ORAL, 7, 14, null, 0))
        );
    }

    private AppliedPrescriptionTemplateResult appliedResult() {
        return new AppliedPrescriptionTemplateResult(
                UUID.randomUUID(),
                "J06.9",
                "Viêm họng",
                List.of(new AppliedPrescriptionTemplateResult.DraftItem(
                        UUID.randomUUID(), "MED001", "Paracetamol", "Paracetamol",
                        "500mg", "viên", "1 viên", 2, AdministrationRoute.ORAL, 7, 14, null)),
                List.of(new AppliedPrescriptionTemplateResult.SkippedItem(
                        UUID.randomUUID(), "Thuốc cũ", "Thuốc đã ngừng sử dụng trong danh mục")),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}

