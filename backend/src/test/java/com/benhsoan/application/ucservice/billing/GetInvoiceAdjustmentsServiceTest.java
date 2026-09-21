package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.InvoiceLine;
import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.InvoiceAdjustmentsResult;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;

class GetInvoiceAdjustmentsServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final InvoiceResultMapper resultMapper = new InvoiceResultMapper();

    private GetInvoiceAdjustmentsService service;

    @BeforeEach
    void setUp() {
        service = new GetInvoiceAdjustmentsService(invoiceRepository, resultMapper);
    }

    @Test
    void returnsOriginalAmountFinalAmountAndAdjustments() {
        UUID originalId = UUID.randomUUID();
        Invoice original = invoice(originalId, "HD000010", InvoiceType.ORIGINAL,
                null, null, new BigDecimal("250000"));
        Invoice adjustment = invoice(UUID.randomUUID(), "HDDC000010", InvoiceType.ADJUSTMENT,
                originalId, "Dieu chinh", new BigDecimal("-20000"));

        when(invoiceRepository.findById(originalId)).thenReturn(Optional.of(original));
        when(invoiceRepository.findAdjustmentsByOriginalInvoiceId(originalId))
                .thenReturn(List.of(adjustment));

        InvoiceAdjustmentsResult result = service.getAdjustments(originalId);

        assertEquals(originalId, result.originalInvoiceId());
        assertEquals(new BigDecimal("250000"), result.originalAmount());
        assertEquals(new BigDecimal("230000"), result.finalAmount());
        assertEquals(1, result.adjustments().size());
    }

    @Test
    void rejectsMissingInvoiceId() {
        assertThrows(ValidationException.class, () -> service.getAdjustments(null));
    }

    @Test
    void throwsInvoiceNotFoundForUnknownInvoice() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(InvoiceNotFoundException.class, () -> service.getAdjustments(id));
    }

    @Test
    void rejectsAdjustmentInvoiceId() {
        UUID originalId = UUID.randomUUID();
        UUID adjustmentId = UUID.randomUUID();
        Invoice adjustment = invoice(adjustmentId, "HDDC000010", InvoiceType.ADJUSTMENT,
                originalId, "Dieu chinh", new BigDecimal("-20000"));

        when(invoiceRepository.findById(adjustmentId)).thenReturn(Optional.of(adjustment));

        assertThrows(ValidationException.class, () -> service.getAdjustments(adjustmentId));
    }

    private Invoice invoice(
            UUID id,
            String code,
            InvoiceType type,
            UUID originalInvoiceId,
            String adjustmentReason,
            BigDecimal totalAmount
    ) {
        UUID visitId = UUID.randomUUID();
        UUID paymentId = type == InvoiceType.ORIGINAL ? UUID.randomUUID() : null;
        InvoiceLine line = InvoiceLine.create(
                UUID.randomUUID(), id, InvoiceLineType.EXAM_FEE, "Phi kham",
                visitId, 1, totalAmount, totalAmount, NOW);
        return Invoice.restore(
                id, code, visitId, paymentId, type,
                originalInvoiceId, adjustmentReason, totalAmount, UUID.randomUUID(), NOW,
                0, null, List.of(line));
    }
}
