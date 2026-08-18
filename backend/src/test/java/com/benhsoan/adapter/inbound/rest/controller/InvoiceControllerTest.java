package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.BillingRestMapper;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.billing.exception.InvoiceAlreadyIssuedException;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.billing.exception.PaymentNotAllowedException;
import com.benhsoan.domain.billing.exception.PaymentNotFoundException;
import com.benhsoan.port.dto.result.InvoiceLineResult;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.dto.result.PayableEncounterResult;
import com.benhsoan.port.dto.result.PaymentResult;
import com.benhsoan.port.dto.result.RefundPaymentResult;
import com.benhsoan.port.inbound.billing.AdjustInvoiceUseCase;
import com.benhsoan.port.inbound.billing.CreateInvoiceUseCase;
import com.benhsoan.port.inbound.billing.GetInvoiceByIdUseCase;
import com.benhsoan.port.inbound.billing.GetPayableEncountersUseCase;
import com.benhsoan.port.inbound.billing.RecordPaymentUseCase;
import com.benhsoan.port.inbound.billing.RefundPaymentUseCase;
import com.benhsoan.port.inbound.billing.SearchInvoicesUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(BillingRestMapper.class)
class InvoiceControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RecordPaymentUseCase recordPaymentUseCase;
    @MockitoBean private CreateInvoiceUseCase createInvoiceUseCase;
    @MockitoBean private AdjustInvoiceUseCase adjustInvoiceUseCase;
    @MockitoBean private RefundPaymentUseCase refundPaymentUseCase;
    @MockitoBean private GetPayableEncountersUseCase getPayableEncountersUseCase;
    @MockitoBean private SearchInvoicesUseCase searchInvoicesUseCase;
    @MockitoBean private GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void recordsPayment() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(recordPaymentUseCase.record(any())).thenReturn(new PaymentResult(
                UUID.randomUUID(),
                visitId,
                new BigDecimal("100000"),
                new BigDecimal("150000"),
                BigDecimal.ZERO,
                new BigDecimal("250000"),
                new BigDecimal("250000"),
                PaymentMethod.CASH,
                PaymentStatus.RECORDED,
                UUID.randomUUID(),
                Instant.parse("2026-08-12T01:00:00Z"),
                Instant.parse("2026-08-12T01:00:00Z")
        ));

        mockMvc.perform(post("/invoices/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "visitId":"%s",
                                  "examFee":100000,
                                  "medicineFee":150000,
                                  "amountPaid":250000,
                                  "paymentMethod":"CASH"
                                }
                                """.formatted(visitId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitId").value(visitId.toString()))
                .andExpect(jsonPath("$.totalAmount").value(250000))
                .andExpect(jsonPath("$.status").value("RECORDED"));
    }

    @Test
    void createsOriginalInvoice() throws Exception {
        UUID visitId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(createInvoiceUseCase.create(any())).thenReturn(originalInvoice(invoiceId, visitId, paymentId));

        mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "visitId":"%s"
                                }
                                """.formatted(visitId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceCode").value("HD000010"))
                .andExpect(jsonPath("$.type").value("ORIGINAL"))
                .andExpect(jsonPath("$.lines[0].lineType").value("EXAM_FEE"));
    }

    @Test
    void returnsPayableEncounters() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(getPayableEncountersUseCase.get(any())).thenReturn(new PageImpl<>(
                List.of(new PayableEncounterResult(
                        visitId,
                        "VIS000010",
                        UUID.randomUUID(),
                        "BN000010",
                        "Nguyen Van A",
                        "Kham tong quat",
                        Instant.parse("2026-08-12T01:30:00Z")
                )),
                PageRequest.of(0, 20),
                1
        ));

        mockMvc.perform(get("/invoices/payable")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].visitId").value(visitId.toString()))
                .andExpect(jsonPath("$.content[0].visitCode").value("VIS000010"))
                .andExpect(jsonPath("$.content[0].patientName").value("Nguyen Van A"));
    }

    @Test
    void searchesInvoices() throws Exception {
        UUID visitId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        when(searchInvoicesUseCase.search(any())).thenReturn(new PageImpl<>(
                List.of(originalInvoice(UUID.randomUUID(), visitId, paymentId)),
                PageRequest.of(0, 20),
                1
        ));

        mockMvc.perform(get("/invoices")
                        .param("invoiceType", "ORIGINAL")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceCode").value("HD000010"))
                .andExpect(jsonPath("$.content[0].type").value("ORIGINAL"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getsInvoiceById() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        when(getInvoiceByIdUseCase.getById(invoiceId)).thenReturn(originalInvoice(invoiceId, visitId, paymentId));

        mockMvc.perform(get("/invoices/{invoiceId}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.invoiceCode").value("HD000010"));
    }

    @Test
    void adjustsInvoice() throws Exception {
        UUID originalInvoiceId = UUID.randomUUID();
        when(adjustInvoiceUseCase.adjust(any())).thenReturn(adjustmentInvoice(originalInvoiceId));

        mockMvc.perform(post("/invoices/{invoiceId}/adjustments", originalInvoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "adjustmentReason":"Dieu chinh giam tien thuoc",
                                  "lines":[
                                    {
                                      "itemName":"Dieu chinh dong 1",
                                      "quantity":1,
                                      "unitPrice":-20000
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceCode").value("HDDC000002"))
                .andExpect(jsonPath("$.type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.originalInvoiceId").value(originalInvoiceId.toString()))
                .andExpect(jsonPath("$.totalAmount").value(-20000));
    }

    @Test
    void refundsPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID originalInvoiceId = UUID.randomUUID();
        Instant refundedAt = Instant.parse("2026-08-18T04:00:00Z");
        when(refundPaymentUseCase.refund(any())).thenReturn(new RefundPaymentResult(
                paymentId,
                visitId,
                PaymentStatus.REFUNDED,
                new BigDecimal("250000"),
                "Patient cancelled",
                actorId,
                refundedAt,
                adjustmentInvoice(originalInvoiceId)
        ));

        mockMvc.perform(post("/invoices/payments/{paymentId}/refund", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Patient cancelled"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.visitId").value(visitId.toString()))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.amountRefunded").value(250000))
                .andExpect(jsonPath("$.refundReason").value("Patient cancelled"))
                .andExpect(jsonPath("$.refundedBy").value(actorId.toString()))
                .andExpect(jsonPath("$.refundedAt").value("2026-08-18T04:00:00Z"))
                .andExpect(jsonPath("$.adjustmentInvoice.type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.adjustmentInvoice.totalAmount").value(-20000));
    }

    @Test
    void rejectsRefundWithBlankReason() throws Exception {
        mockMvc.perform(post("/invoices/payments/{paymentId}/refund", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());

        verifyNoInteractions(refundPaymentUseCase);
    }

    @Test
    void mapsMissingPaymentDuringRefundTo404() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(refundPaymentUseCase.refund(any())).thenThrow(new PaymentNotFoundException(paymentId));

        mockMvc.perform(post("/invoices/payments/{paymentId}/refund", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Patient cancelled"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found: " + paymentId));
    }

    @Test
    void mapsMissingOriginalInvoiceDuringRefundTo404() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(refundPaymentUseCase.refund(any()))
                .thenThrow(new InvoiceNotFoundException(paymentId, true));

        mockMvc.perform(post("/invoices/payments/{paymentId}/refund", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Patient cancelled"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Original invoice not found for payment: " + paymentId
                ));
    }

    @Test
    void mapsInvalidRefundStateTo409() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(refundPaymentUseCase.refund(any())).thenThrow(
                new PaymentNotAllowedException("Payment has already been refunded.")
        );

        mockMvc.perform(post("/invoices/payments/{paymentId}/refund", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Patient cancelled"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Payment has already been refunded."));
    }

    @Test
    void rejectsSearchWhenCreatedRangeIsInvalid() throws Exception {
        mockMvc.perform(get("/invoices")
                        .param("createdFrom", "2026-08-12T03:00:00Z")
                        .param("createdTo", "2026-08-12T01:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("createdFrom must be before or equal to createdTo."));

        verifyNoInteractions(searchInvoicesUseCase);
    }

    @Test
    void mapsNotFoundExceptionTo404() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(getInvoiceByIdUseCase.getById(invoiceId)).thenThrow(new InvoiceNotFoundException(invoiceId));

        mockMvc.perform(get("/invoices/{invoiceId}", invoiceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invoice not found: " + invoiceId));
    }

    @Test
    void mapsBusinessConflictTo409() throws Exception {
        UUID visitId = UUID.randomUUID();
        when(createInvoiceUseCase.create(any())).thenThrow(new InvoiceAlreadyIssuedException(visitId));

        mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "visitId":"%s"
                                }
                                """.formatted(visitId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An invoice already exists for visit: " + visitId));
    }

    private static InvoiceResult originalInvoice(UUID invoiceId, UUID visitId, UUID paymentId) {
        Instant now = Instant.parse("2026-08-12T02:00:00Z");
        return new InvoiceResult(
                invoiceId,
                "HD000010",
                visitId,
                paymentId,
                InvoiceType.ORIGINAL,
                null,
                null,
                new BigDecimal("250000"),
                UUID.randomUUID(),
                now,
                List.of(
                        new InvoiceLineResult(
                                UUID.randomUUID(),
                                invoiceId,
                                InvoiceLineType.EXAM_FEE,
                                "Phi kham",
                                visitId,
                                1,
                                new BigDecimal("100000"),
                                new BigDecimal("100000"),
                                now
                        ),
                        new InvoiceLineResult(
                                UUID.randomUUID(),
                                invoiceId,
                                InvoiceLineType.MEDICINE_FEE,
                                "Tien thuoc",
                                paymentId,
                                1,
                                new BigDecimal("150000"),
                                new BigDecimal("150000"),
                                now
                        )
                )
        );
    }

    private static InvoiceResult adjustmentInvoice(UUID originalInvoiceId) {
        UUID invoiceId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-12T03:00:00Z");
        return new InvoiceResult(
                invoiceId,
                "HDDC000002",
                UUID.randomUUID(),
                null,
                InvoiceType.ADJUSTMENT,
                originalInvoiceId,
                "Dieu chinh giam tien thuoc",
                new BigDecimal("-20000"),
                UUID.randomUUID(),
                now,
                List.of(
                        new InvoiceLineResult(
                                UUID.randomUUID(),
                                invoiceId,
                                InvoiceLineType.ADJUSTMENT,
                                "Dieu chinh dong 1",
                                originalInvoiceId,
                                1,
                                new BigDecimal("-20000"),
                                new BigDecimal("-20000"),
                                now
                        )
                )
        );
    }
}
