package com.benhsoan.infrastructure.pdf;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.benhsoan.port.dto.result.billing.InvoicePrintDocument;
import com.benhsoan.port.dto.result.billing.InvoicePrintDocument.InvoicePrintLine;

class PdfBoxInvoicePdfRendererTest {

    @Test
    void rendersValidPdfDocument() {
        PdfBoxInvoicePdfRenderer renderer = new PdfBoxInvoicePdfRenderer();

        List<InvoicePrintLine> lines = List.of(
                new InvoicePrintLine(1, "Khám chuyên khoa Nội", "SERVICE", 1, new BigDecimal("150000"), new BigDecimal("150000")),
                new InvoicePrintLine(2, "Paracetamol 500mg", "MEDICINE", 20, new BigDecimal("5000"), new BigDecimal("100000"))
        );

        InvoicePrintDocument document = new InvoicePrintDocument(
                "Phòng khám Đa khoa Hoàn Mỹ",
                "123 Nguyễn Huệ, Q.1, TP.HCM",
                "02838445566",
                "HD-20260921-0001",
                "ORIGINAL",
                null,
                null,
                "BN-20260921-0001",
                "Nguyễn Văn Bệnh Nhân",
                "15/05/1990",
                "MALE",
                "0901234567",
                "KB-20260921-0012",
                Instant.parse("2026-09-21T08:00:00Z"),
                "BS. Trần Văn Bác Sĩ",
                "Khoa Nội",
                Instant.parse("2026-09-21T08:30:00Z"),
                "Lễ tân Nguyễn Thị Lễ Tân",
                lines,
                new BigDecimal("250000"),
                Instant.parse("2026-09-21T08:35:00Z")
        );

        byte[] pdfBytes = renderer.render(document);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertTrue(header.startsWith("%PDF"), "PDF bytes should start with %PDF header");
    }

    @Test
    void rendersAdjustmentInvoiceWithContinuationPages() {
        PdfBoxInvoicePdfRenderer renderer = new PdfBoxInvoicePdfRenderer();

        List<InvoicePrintLine> manyLines = new java.util.ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            manyLines.add(new InvoicePrintLine(
                    i,
                    "Khoản mục chi phí xét nghiệm kiểm tra số " + i,
                    "SERVICE",
                    1,
                    new BigDecimal("10000"),
                    new BigDecimal("10000")
            ));
        }

        InvoicePrintDocument document = new InvoicePrintDocument(
                "Phòng khám Đa khoa Hoàn Mỹ",
                "123 Nguyễn Huệ, Q.1, TP.HCM",
                "02838445566",
                "HD-20260921-0002",
                "ADJUSTMENT",
                "HD-20260921-0001",
                "Điều chỉnh giảm tiền khám do áp dụng voucher giảm giá",
                "BN-20260921-0001",
                "Nguyễn Văn Bệnh Nhân",
                "15/05/1990",
                "MALE",
                "0901234567",
                "KB-20260921-0012",
                Instant.parse("2026-09-21T08:00:00Z"),
                "BS. Trần Văn Bác Sĩ",
                "Khoa Nội",
                Instant.parse("2026-09-21T09:00:00Z"),
                "Lễ tân Nguyễn Thị Lễ Tân",
                manyLines,
                new BigDecimal("600000"),
                Instant.parse("2026-09-21T09:05:00Z")
        );

        byte[] pdfBytes = renderer.render(document);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        String header = new String(pdfBytes, 0, Math.min(pdfBytes.length, 5));
        assertTrue(header.startsWith("%PDF"));
    }
}
