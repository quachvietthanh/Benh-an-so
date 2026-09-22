package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalClinicalResultRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.result.portal.ClinicalResultPrintResult;
import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultDetailResult;
import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultSummaryResult;
import com.benhsoan.port.inbound.portal.ExportPatientPortalClinicalResultUseCase;
import com.benhsoan.port.inbound.portal.GetPatientPortalClinicalResultDetailUseCase;
import com.benhsoan.port.inbound.portal.GetPatientPortalClinicalResultsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientPortalClinicalResultController.class)
@Import({
        PatientPortalClinicalResultRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class PatientPortalClinicalResultControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetPatientPortalClinicalResultsUseCase getPatientPortalClinicalResultsUseCase;
    @MockitoBean private GetPatientPortalClinicalResultDetailUseCase getPatientPortalClinicalResultDetailUseCase;
    @MockitoBean private ExportPatientPortalClinicalResultUseCase exportPatientPortalClinicalResultUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void getClinicalResults_returns200WithList() throws Exception {
        UUID resultId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        when(getPatientPortalClinicalResultsUseCase.getClinicalResults(visitId)).thenReturn(List.of(
                new PatientPortalClinicalResultSummaryResult(
                        resultId,
                        itemId,
                        visitId,
                        "XN-MAU-01",
                        "Tổng phân tích tế bào máu",
                        "NUMBER",
                        new BigDecimal("14.2"),
                        new BigDecimal("12.0"),
                        new BigDecimal("16.5"),
                        null,
                        "g/dL",
                        "12.0 - 16.5",
                        "NORMAL",
                        "Bình thường",
                        "FINAL",
                        "BS. Nguyễn Văn A",
                        Instant.parse("2026-09-22T08:30:00Z"),
                        false
                )
        ));

        mockMvc.perform(get("/patient-portal/clinical-results")
                        .param("visitId", visitId.toString())
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clinicalResultId").value(resultId.toString()))
                .andExpect(jsonPath("$[0].serviceCode").value("XN-MAU-01"))
                .andExpect(jsonPath("$[0].status").value("FINAL"))
                .andExpect(jsonPath("$[0].doctorName").value("BS. Nguyễn Văn A"));
    }

    @Test
    void getClinicalResults_whenEmpty_returns200WithEmptyArray() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(getPatientPortalClinicalResultsUseCase.getClinicalResults(visitId)).thenReturn(List.of());

        mockMvc.perform(get("/patient-portal/clinical-results")
                        .param("visitId", visitId.toString())
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getClinicalResultsByVisit_returns200WithList() throws Exception {
        UUID resultId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        when(getPatientPortalClinicalResultsUseCase.getClinicalResults(visitId)).thenReturn(List.of(
                new PatientPortalClinicalResultSummaryResult(
                        resultId,
                        UUID.randomUUID(),
                        visitId,
                        "XN-GLU-01",
                        "Glucose",
                        "NUMBER",
                        new BigDecimal("5.5"),
                        new BigDecimal("3.9"),
                        new BigDecimal("6.4"),
                        null,
                        "mmol/L",
                        "3.9 - 6.4",
                        "NORMAL",
                        "Bình thường",
                        "FINAL",
                        "BS. B",
                        Instant.now(),
                        false
                )
        ));

        mockMvc.perform(get("/patient-portal/visits/" + visitId + "/clinical-results")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clinicalResultId").value(resultId.toString()))
                .andExpect(jsonPath("$[0].serviceCode").value("XN-GLU-01"));
    }

    @Test
    void getClinicalResultDetail_returns200() throws Exception {
        UUID resultId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        when(getPatientPortalClinicalResultDetailUseCase.getClinicalResultDetail(resultId)).thenReturn(
                new PatientPortalClinicalResultDetailResult(
                        resultId,
                        UUID.randomUUID(),
                        visitId,
                        "KB-001",
                        Instant.now(),
                        "XN-MAU-01",
                        "Tổng phân tích tế bào máu",
                        "NUMBER",
                        new BigDecimal("14.2"),
                        new BigDecimal("12.0"),
                        new BigDecimal("16.5"),
                        null,
                        "g/dL",
                        "12.0 - 16.5",
                        "NORMAL",
                        "Bình thường",
                        "FINAL",
                        "BS. Chỉ Định",
                        "BS. Thực Hiện",
                        "Khoa Xét nghiệm",
                        Instant.now(),
                        List.of()
                )
        );

        mockMvc.perform(get("/patient-portal/clinical-results/" + resultId)
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clinicalResultId").value(resultId.toString()))
                .andExpect(jsonPath("$.serviceCode").value("XN-MAU-01"))
                .andExpect(jsonPath("$.orderingDoctorName").value("BS. Chỉ Định"))
                .andExpect(jsonPath("$.performingDoctorName").value("BS. Thực Hiện"));
    }

    @Test
    void downloadResult_returns200WithPdf() throws Exception {
        UUID resultId = UUID.randomUUID();
        byte[] pdfContent = new byte[]{1, 2, 3, 4};

        when(exportPatientPortalClinicalResultUseCase.exportByResult(resultId)).thenReturn(
                new ClinicalResultPrintResult("ket-qua-XN-MAU-01.pdf", "application/pdf", pdfContent)
        );

        mockMvc.perform(get("/patient-portal/clinical-results/" + resultId + "/download")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ket-qua-XN-MAU-01.pdf\""))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/pdf")));
    }

    @Test
    void downloadVisitResults_returns200WithPdf() throws Exception {
        UUID visitId = UUID.randomUUID();
        byte[] pdfContent = new byte[]{5, 6, 7, 8};

        when(exportPatientPortalClinicalResultUseCase.exportByVisit(visitId)).thenReturn(
                new ClinicalResultPrintResult("ket-qua-can-lam-sang-KB-001.pdf", "application/pdf", pdfContent)
        );

        mockMvc.perform(get("/patient-portal/visits/" + visitId + "/clinical-results/download")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ket-qua-can-lam-sang-KB-001.pdf\""))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/pdf")));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("SEC-03 / Test Gap 2: Unauthenticated user accessing patient portal returns 401 Unauthorized")
    void getClinicalResults_unauthenticated_returns401() throws Exception {
        UUID visitId = UUID.randomUUID();
        mockMvc.perform(get("/patient-portal/clinical-results")
                        .param("visitId", visitId.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("SEC-04 / Test Gap 2: Non-patient role (ROLE_DOCTOR) accessing patient portal returns 403 Forbidden")
    void getClinicalResults_wrongRole_returns403() throws Exception {
        UUID visitId = UUID.randomUUID();
        mockMvc.perform(get("/patient-portal/clinical-results")
                        .param("visitId", visitId.toString())
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }
}
