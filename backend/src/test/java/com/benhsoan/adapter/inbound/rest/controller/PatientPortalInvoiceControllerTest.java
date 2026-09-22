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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalInvoiceRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.result.portal.InvoicePrintResult;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceDetailResult;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceDetailResult.InvoiceLineItemView;
import com.benhsoan.port.dto.result.portal.PatientPortalInvoiceSummaryResult;
import com.benhsoan.port.inbound.portal.ExportPatientPortalInvoiceUseCase;
import com.benhsoan.port.inbound.portal.GetPatientPortalInvoiceDetailUseCase;
import com.benhsoan.port.inbound.portal.GetPatientPortalInvoicesUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientPortalInvoiceController.class)
@Import({
        PatientPortalInvoiceRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class PatientPortalInvoiceControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetPatientPortalInvoicesUseCase getPatientPortalInvoicesUseCase;
    @MockitoBean private GetPatientPortalInvoiceDetailUseCase getPatientPortalInvoiceDetailUseCase;
    @MockitoBean private ExportPatientPortalInvoiceUseCase exportPatientPortalInvoiceUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void getInvoices_returns200WithList() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        when(getPatientPortalInvoicesUseCase.getInvoices(null, null)).thenReturn(List.of(
                new PatientPortalInvoiceSummaryResult(
                        invoiceId,
                        "HD-20260921-0001",
                        "ORIGINAL",
                        new BigDecimal("250000"),
                        Instant.parse("2026-09-21T08:30:00Z"),
                        visitId,
                        "KB-001",
                        Instant.parse("2026-09-21T08:00:00Z"),
                        "BS. Trần Văn A",
                        "Khoa Nội",
                        2
                )
        ));

        mockMvc.perform(get("/patient-portal/invoices")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceId").value(invoiceId.toString()))
                .andExpect(jsonPath("$[0].invoiceCode").value("HD-20260921-0001"))
                .andExpect(jsonPath("$[0].doctorName").value("BS. Trần Văn A"))
                .andExpect(jsonPath("$[0].totalAmount").value(250000))
                .andExpect(jsonPath("$[0].itemCount").value(2));
    }

    @Test
    void getInvoices_whenEmpty_returns200WithEmptyArray() throws Exception {
        when(getPatientPortalInvoicesUseCase.getInvoices(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/patient-portal/invoices")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getInvoices_withLimitAndVisitId_passesQueryParamsToUseCase() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(getPatientPortalInvoicesUseCase.getInvoices(visitId, 10)).thenReturn(List.of());

        mockMvc.perform(get("/patient-portal/invoices")
                        .param("visitId", visitId.toString())
                        .param("limit", "10")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getInvoiceDetail_returns200WithDetail() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();

        when(getPatientPortalInvoiceDetailUseCase.getInvoiceDetail(invoiceId)).thenReturn(
                new PatientPortalInvoiceDetailResult(
                        invoiceId,
                        "HD-20260921-0001",
                        "ORIGINAL",
                        null,
                        null,
                        null,
                        new BigDecimal("150000"),
                        Instant.parse("2026-09-21T08:30:00Z"),
                        "Lễ tân B",
                        visitId,
                        "KB-001",
                        Instant.parse("2026-09-21T08:00:00Z"),
                        "BS. Trần Văn A",
                        "Khoa Nội",
                        List.of(new InvoiceLineItemView(
                                UUID.randomUUID(),
                                "SERVICE",
                                "Khám nội tổng quát",
                                1,
                                new BigDecimal("150000"),
                                new BigDecimal("150000")
                        ))
                )
        );

        mockMvc.perform(get("/patient-portal/invoices/{invoiceId}", invoiceId)
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceCode").value("HD-20260921-0001"))
                .andExpect(jsonPath("$.items[0].itemName").value("Khám nội tổng quát"));
    }

    @Test
    void getInvoiceDetail_whenAccessDenied_returns403() throws Exception {
        UUID invoiceId = UUID.randomUUID();

        when(getPatientPortalInvoiceDetailUseCase.getInvoiceDetail(invoiceId))
                .thenThrow(new AccessDeniedException("Patient may only access their own data."));

        mockMvc.perform(get("/patient-portal/invoices/{invoiceId}", invoiceId)
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void downloadInvoice_returns200WithPdfStream() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        byte[] fakePdf = "%PDF-1.4 fake content".getBytes();

        when(exportPatientPortalInvoiceUseCase.export(invoiceId)).thenReturn(
                new InvoicePrintResult("hoa-don-HD-001.pdf", "application/pdf", fakePdf)
        );

        mockMvc.perform(get("/patient-portal/invoices/{invoiceId}/download", invoiceId)
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"hoa-don-HD-001.pdf\""))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/pdf")));
    }
}
