package com.benhsoan.domain.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.billing.exception.InvoiceAlreadyIssuedException;
import com.benhsoan.domain.billing.exception.InvoiceUnauthorizedAdjustmentException;
import com.benhsoan.domain.billing.exception.PaymentRequiredForInvoiceException;

@DisplayName("Invoice Domain Tests")
class InvoiceTest {

    @Test
    @DisplayName("createOriginal should create invoice after payment is recorded")
    void createOriginalShouldSucceed() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.createOriginal(
                invoiceId,
                "HD-0001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-11T03:30:00Z"),
                List.of(
                        line(invoiceId, InvoiceLineType.EXAM_FEE, "Exam fee", 1, "100000"),
                        line(invoiceId, InvoiceLineType.MEDICINE_FEE, "Medicine fee", 1, "150000")
                ),
                true,
                false
        );

        assertEquals(InvoiceType.ORIGINAL, invoice.getType());
        assertEquals(new BigDecimal("250000"), invoice.getTotalAmount());
        assertTrue(invoice.isOriginal());
    }

    @Test
    @DisplayName("createOriginal should create invoice with discount line and discount metadata")
    void createOriginalShouldSucceedWithDiscount() {
        UUID invoiceId = UUID.randomUUID();
        UUID discountRequestId = UUID.randomUUID();
        Invoice invoice = Invoice.createOriginal(
                invoiceId,
                "HD-0001-DISC",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-11T03:30:00Z"),
                List.of(
                        line(invoiceId, InvoiceLineType.EXAM_FEE, "Exam fee", 1, "100000"),
                        line(invoiceId, InvoiceLineType.MEDICINE_FEE, "Medicine fee", 1, "150000"),
                        line(invoiceId, InvoiceLineType.DISCOUNT, "Giảm giá 20%", 1, "-50000")
                ),
                true,
                false,
                new BigDecimal("50000"),
                discountRequestId
        );

        assertEquals(InvoiceType.ORIGINAL, invoice.getType());
        assertEquals(new BigDecimal("200000"), invoice.getTotalAmount());
        assertEquals(new BigDecimal("50000"), invoice.getDiscountAmount());
        assertEquals(discountRequestId, invoice.getDiscountRequestId());
        assertTrue(invoice.isOriginal());
    }

    @Test
    @DisplayName("createOriginal should succeed with zero total amount for full free exemption")
    void createOriginalShouldSucceedWithZeroTotalForFullFreeExemption() {
        UUID invoiceId = UUID.randomUUID();
        UUID discountRequestId = UUID.randomUUID();
        Invoice invoice = Invoice.createOriginal(
                invoiceId,
                "HD-0001-FREE",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-11T03:30:00Z"),
                List.of(
                        line(invoiceId, InvoiceLineType.EXAM_FEE, "Exam fee", 1, "100000"),
                        line(invoiceId, InvoiceLineType.DISCOUNT, "Miễn phí 100%", 1, "-100000")
                ),
                true,
                false,
                new BigDecimal("100000"),
                discountRequestId
        );

        assertEquals(InvoiceType.ORIGINAL, invoice.getType());
        assertEquals(BigDecimal.ZERO, invoice.getTotalAmount());
        assertEquals(new BigDecimal("100000"), invoice.getDiscountAmount());
        assertEquals(discountRequestId, invoice.getDiscountRequestId());
        assertTrue(invoice.isOriginal());
    }

    @Test
    @DisplayName("createOriginal should reject duplicate invoice for same visit")
    void createOriginalShouldRejectDuplicateInvoice() {
        UUID invoiceId = UUID.randomUUID();

        assertThrows(
                InvoiceAlreadyIssuedException.class,
                () -> Invoice.createOriginal(
                        invoiceId,
                        "HD-0002",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse("2026-08-11T03:30:00Z"),
                        List.of(line(invoiceId, InvoiceLineType.EXAM_FEE, "Exam fee", 1, "100000")),
                        true,
                        true
                )
        );
    }

    @Test
    @DisplayName("createOriginal should reject invoice when payment has not been recorded")
    void createOriginalShouldRejectMissingPayment() {
        UUID invoiceId = UUID.randomUUID();

        assertThrows(
                PaymentRequiredForInvoiceException.class,
                () -> Invoice.createOriginal(
                        invoiceId,
                        "HD-0003",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse("2026-08-11T03:30:00Z"),
                        List.of(line(invoiceId, InvoiceLineType.EXAM_FEE, "Exam fee", 1, "100000")),
                        false,
                        false
                )
        );
    }

    @Test
    @DisplayName("createAdjustment should create linked adjustment invoice with reason")
    void createAdjustmentShouldSucceed() {
        UUID invoiceId = UUID.randomUUID();
        UUID originalInvoiceId = UUID.randomUUID();

        Invoice invoice = Invoice.createAdjustment(
                invoiceId,
                "HD-DC-0001",
                UUID.randomUUID(),
                originalInvoiceId,
                "Corrected medicine fee",
                UUID.randomUUID(),
                Instant.parse("2026-08-11T04:00:00Z"),
                List.of(line(invoiceId, InvoiceLineType.ADJUSTMENT, "Adjustment", 1, "-50000")),
                true
        );

        assertEquals(InvoiceType.ADJUSTMENT, invoice.getType());
        assertEquals(originalInvoiceId, invoice.getOriginalInvoiceId());
        assertEquals(new BigDecimal("-50000"), invoice.getTotalAmount());
        assertTrue(invoice.isAdjustment());
    }

    @Test
    @DisplayName("createAdjustment should require a reason")
    void createAdjustmentShouldRequireReason() {
        UUID invoiceId = UUID.randomUUID();

        assertThrows(
                ValidationException.class,
                () -> Invoice.createAdjustment(
                        invoiceId,
                        "HD-DC-0002",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        " ",
                        UUID.randomUUID(),
                        Instant.parse("2026-08-11T04:00:00Z"),
                        List.of(line(invoiceId, InvoiceLineType.ADJUSTMENT, "Adjustment", 1, "-50000")),
                        true
                )
        );
    }

    @Test
    @DisplayName("createAdjustment should reject unauthorized adjustment")
    void createAdjustmentShouldRejectUnauthorizedActor() {
        UUID invoiceId = UUID.randomUUID();

        assertThrows(
                InvoiceUnauthorizedAdjustmentException.class,
                () -> Invoice.createAdjustment(
                        invoiceId,
                        "HD-DC-0003",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Corrected exam fee",
                        UUID.randomUUID(),
                        Instant.parse("2026-08-11T04:00:00Z"),
                        List.of(line(invoiceId, InvoiceLineType.ADJUSTMENT, "Adjustment", 1, "50000")),
                        false
                )
        );
    }

    @Test
    @DisplayName("invoice line should reject zero unit price")
    void invoiceLineShouldRejectZeroUnitPrice() {
        assertThrows(
                ValidationException.class,
                () -> InvoiceLine.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        InvoiceLineType.ADJUSTMENT,
                        "Adjustment",
                        null,
                        1,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        Instant.parse("2026-08-11T04:00:00Z")
                )
        );
    }

    private static InvoiceLine line(
            UUID invoiceId,
            InvoiceLineType lineType,
            String itemName,
            int quantity,
            String unitPrice
    ) {
        BigDecimal amount = new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity));
        return InvoiceLine.create(
                UUID.randomUUID(),
                invoiceId,
                lineType,
                itemName,
                null,
                quantity,
                new BigDecimal(unitPrice),
                amount,
                Instant.parse("2026-08-11T03:30:00Z")
        );
    }
}
