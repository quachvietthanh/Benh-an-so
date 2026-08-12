package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.BillingRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.result.InvoiceLineResult;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.dto.result.PaymentResult;
import com.benhsoan.port.inbound.billing.AdjustInvoiceUseCase;
import com.benhsoan.port.inbound.billing.CreateInvoiceUseCase;
import com.benhsoan.port.inbound.billing.GetInvoiceByIdUseCase;
import com.benhsoan.port.inbound.billing.GetPayableEncountersUseCase;
import com.benhsoan.port.inbound.billing.RecordPaymentUseCase;
import com.benhsoan.port.inbound.billing.SearchInvoicesUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = InvoiceController.class)
@Import({BillingRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class})
class InvoiceSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RecordPaymentUseCase recordPaymentUseCase;
    @MockitoBean private CreateInvoiceUseCase createInvoiceUseCase;
    @MockitoBean private AdjustInvoiceUseCase adjustInvoiceUseCase;
    @MockitoBean private GetPayableEncountersUseCase getPayableEncountersUseCase;
    @MockitoBean private SearchInvoicesUseCase searchInvoicesUseCase;
    @MockitoBean private GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;

    @Test
    void allowsAdminsAndReceptionistsToReadInvoices() throws Exception {
        when(searchInvoicesUseCase.search(any())).thenReturn(Page.empty());

        for (String role : new String[] {"ADMIN", "RECEPTIONIST"}) {
            mockMvc.perform(get("/invoices").with(user("tester").roles(role)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/invoices").with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminsAndReceptionistsToCreateInvoicesAndPayments() throws Exception {
        UUID visitId = UUID.fromString("d0000000-0000-0000-0000-000000000001");
        UUID paymentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-12T02:00:00Z");

        when(recordPaymentUseCase.record(any())).thenReturn(new PaymentResult(
                paymentId,
                visitId,
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.CASH,
                PaymentStatus.RECORDED,
                UUID.randomUUID(),
                now,
                now
        ));
        when(createInvoiceUseCase.create(any())).thenReturn(new InvoiceResult(
                invoiceId,
                "HD000020",
                visitId,
                paymentId,
                InvoiceType.ORIGINAL,
                null,
                null,
                new BigDecimal("250000"),
                UUID.randomUUID(),
                now,
                List.of(new InvoiceLineResult(
                        UUID.randomUUID(),
                        invoiceId,
                        InvoiceLineType.EXAM_FEE,
                        "Phi kham",
                        visitId,
                        1,
                        new BigDecimal("100000"),
                        new BigDecimal("100000"),
                        now
                ))
        ));

        String paymentBody = """
                {
                  "visitId":"d0000000-0000-0000-0000-000000000001",
                  "examFee":100000,
                  "medicineFee":150000,
                  "amountPaid":250000,
                  "paymentMethod":"CASH"
                }
                """;

        mockMvc.perform(post("/invoices/payments")
                        .with(user("receptionist").roles("RECEPTIONIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/invoices/payments")
                        .with(user("doctor").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/invoices")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"visitId":"d0000000-0000-0000-0000-000000000001"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void onlyAllowsAdminToAdjustInvoices() throws Exception {
        UUID originalInvoiceId = UUID.fromString("23100000-0000-0000-0000-000000000001");
        UUID adjustmentInvoiceId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-12T03:00:00Z");
        when(adjustInvoiceUseCase.adjust(any())).thenReturn(new InvoiceResult(
                adjustmentInvoiceId,
                "HDDC000010",
                UUID.randomUUID(),
                null,
                InvoiceType.ADJUSTMENT,
                originalInvoiceId,
                "Dieu chinh giam tien thuoc",
                new BigDecimal("-20000"),
                UUID.randomUUID(),
                now,
                List.of(new InvoiceLineResult(
                        UUID.randomUUID(),
                        adjustmentInvoiceId,
                        InvoiceLineType.ADJUSTMENT,
                        "Dieu chinh dong 1",
                        originalInvoiceId,
                        1,
                        new BigDecimal("-20000"),
                        new BigDecimal("-20000"),
                        now
                ))
        ));

        String body = """
                {
                  "adjustmentReason":"Dieu chinh giam tien thuoc",
                  "lines":[{"itemName":"Dieu chinh dong 1","quantity":1,"unitPrice":-20000}]
                }
                """;

        mockMvc.perform(post("/invoices/{invoiceId}/adjustments", "23100000-0000-0000-0000-000000000001")
                        .with(user("receptionist").roles("RECEPTIONIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/invoices/{invoiceId}/adjustments", "23100000-0000-0000-0000-000000000001")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
