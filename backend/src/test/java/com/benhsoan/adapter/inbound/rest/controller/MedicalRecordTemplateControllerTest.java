package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordTemplateRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordFieldCode;
import com.benhsoan.port.dto.command.medicalrecord.CreateMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordTemplateStatusCommand;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;
import com.benhsoan.port.dto.result.MedicalRecordTemplateSectionResult;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordTemplateUseCase;
import com.benhsoan.port.inbound.medicalrecord.MedicalRecordTemplateQueryUseCase;
import com.benhsoan.port.inbound.medicalrecord.SetMedicalRecordTemplateDefaultUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordTemplateStatusUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordTemplateUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicalRecordTemplateController.class)
@Import({
        AopAutoConfiguration.class,
        MedicalRecordTemplateControllerTest.AspectTestConfig.class,
        MedicalRecordTemplateRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class MedicalRecordTemplateControllerTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("12000000-0000-0000-0000-000000000001");
    private static final UUID SPECIALTY_ID = UUID.fromString("12000000-0000-0000-0000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-08-26T00:00:00Z");

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MedicalRecordTemplateQueryUseCase queryUseCase;
    @MockitoBean private CreateMedicalRecordTemplateUseCase createUseCase;
    @MockitoBean private UpdateMedicalRecordTemplateUseCase updateUseCase;
    @MockitoBean private SetMedicalRecordTemplateDefaultUseCase setDefaultUseCase;
    @MockitoBean private UpdateMedicalRecordTemplateStatusUseCase updateStatusUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void exposesSearchAndGetForTemplateManagers() throws Exception {
        when(queryUseCase.search(any())).thenReturn(List.of(result(true, true)));
        when(queryUseCase.getById(TEMPLATE_ID)).thenReturn(result(true, true));

        mockMvc.perform(get("/system/medical-record-templates")
                        .param("specialtyId", SPECIALTY_ID.toString())
                        .param("active", "true")
                        .with(withManagePermission()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TEMPLATE_ID.toString()))
                .andExpect(jsonPath("$[0].specialty.code").value("GENERAL"))
                .andExpect(jsonPath("$[0].currentVersionNo").value(1));

        mockMvc.perform(get("/system/medical-record-templates/{templateId}", TEMPLATE_ID)
                        .with(withManagePermission()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].fieldCode").value("CHIEF_COMPLAINT"));
    }

    @Test
    void createsUpdatesSetsDefaultAndDeactivatesTemplate() throws Exception {
        when(createUseCase.create(any())).thenReturn(result(true, true));
        when(updateUseCase.update(any())).thenReturn(result(true, true));
        when(setDefaultUseCase.setDefault(TEMPLATE_ID)).thenReturn(result(true, true));
        when(updateStatusUseCase.updateStatus(any())).thenReturn(result(false, false));

        mockMvc.perform(post("/system/medical-record-templates")
                        .with(withManagePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateBody("General template", true)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/system/medical-record-templates/" + TEMPLATE_ID))
                .andExpect(jsonPath("$.defaultTemplate").value(true));

        ArgumentCaptor<CreateMedicalRecordTemplateCommand> createCaptor =
                ArgumentCaptor.forClass(CreateMedicalRecordTemplateCommand.class);
        verify(createUseCase).create(createCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(SPECIALTY_ID, createCaptor.getValue().specialtyId());
        org.junit.jupiter.api.Assertions.assertTrue(createCaptor.getValue().makeDefault());

        mockMvc.perform(put("/system/medical-record-templates/{templateId}", TEMPLATE_ID)
                        .with(withManagePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated template","changeNote":"Clarify fields",
                                  "sections":[{"fieldCode":"CHIEF_COMPLAINT","label":"Complaint","required":true,"displayOrder":1}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("General template"));

        mockMvc.perform(patch("/system/medical-record-templates/{templateId}/default", TEMPLATE_ID)
                        .with(withManagePermission()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultTemplate").value(true));

        UUID replacementId = UUID.randomUUID();
        mockMvc.perform(patch("/system/medical-record-templates/{templateId}/status", TEMPLATE_ID)
                        .with(withManagePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false,\"replacementTemplateId\":\"" + replacementId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        ArgumentCaptor<UpdateMedicalRecordTemplateStatusCommand> statusCaptor =
                ArgumentCaptor.forClass(UpdateMedicalRecordTemplateStatusCommand.class);
        verify(updateStatusUseCase).updateStatus(statusCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(replacementId, statusCaptor.getValue().replacementTemplateId());
    }

    @Test
    void rejectsDoctorWithoutTemplateManagementPermission() throws Exception {
        mockMvc.perform(get("/system/medical-record-templates").with(withDoctorPermission()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/system/medical-record-templates")
                        .with(withDoctorPermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateBody("General template", false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(queryUseCase, createUseCase, updateUseCase, setDefaultUseCase, updateStatusUseCase);
    }

    @Test
    void rejectsMissingStatusInsteadOfTreatingItAsDeactivate() throws Exception {
        mockMvc.perform(patch("/system/medical-record-templates/{templateId}/status", TEMPLATE_ID)
                        .with(withManagePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(updateStatusUseCase);
    }

    @Test
    void exposesTheDocumentedValidationAndMalformedInputErrorCodes() throws Exception {
        mockMvc.perform(post("/system/medical-record-templates")
                        .with(withManagePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specialtyId":"%s","name":"General template",
                                  "sections":[{"fieldCode":"NOT_A_FIELD","label":"Complaint","required":true,"displayOrder":1}]}
                                """.formatted(SPECIALTY_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/system/medical-record-templates/not-a-uuid").with(withManagePermission()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        mockMvc.perform(post("/system/medical-record-templates")
                        .with(withManagePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor withManagePermission() {
        return user("admin").roles("ADMIN")
                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_TEMPLATE_MANAGE"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor withDoctorPermission() {
        return user("doctor").roles("DOCTOR")
                .authorities(new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ"));
    }

    private String templateBody(String name, boolean makeDefault) {
        return """
                {"specialtyId":"%s","name":"%s","makeDefault":%s,
                  "sections":[{"fieldCode":"CHIEF_COMPLAINT","label":"Complaint","required":true,"displayOrder":1}]}
                """.formatted(SPECIALTY_ID, name, makeDefault);
    }

    private MedicalRecordTemplateResult result(boolean active, boolean defaultTemplate) {
        return new MedicalRecordTemplateResult(TEMPLATE_ID,
                new SpecialtyResult(SPECIALTY_ID, "GENERAL", "General", true), "General template", active,
                defaultTemplate, 1,
                List.of(new MedicalRecordTemplateSectionResult(MedicalRecordFieldCode.CHIEF_COMPLAINT,
                        "Complaint", true, 1)), CREATED_AT, CREATED_AT);
    }
}
