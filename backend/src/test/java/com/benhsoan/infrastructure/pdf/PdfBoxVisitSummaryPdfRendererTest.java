package com.benhsoan.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import com.benhsoan.port.dto.result.VisitSummaryPrintDocument;

class PdfBoxVisitSummaryPdfRendererTest {

    @Test
    void rendersVisitSummaryToValidPdf() throws Exception {
        VisitSummaryPrintDocument document = new VisitSummaryPrintDocument(
                "Phòng khám Đa khoa Hoàn Mỹ",
                "123 Hoàng Văn Thụ, Tân Bình, TP.HCM",
                "02838445566",
                "BN-2026-0001",
                "Nguyễn Văn A",
                "1990-05-15",
                "MALE",
                "0901234567",
                "KB-20260820-0001",
                Instant.parse("2026-08-20T08:30:00Z"),
                "BS. Trần Văn B",
                List.of(
                        new VisitSummaryPrintDocument.Diagnosis("J00", "Viêm mũi họng cấp (cảm thường)", true),
                        new VisitSummaryPrintDocument.Diagnosis("R05", "Ho kéo dài", false)
                ),
                List.of(
                        new VisitSummaryPrintDocument.ClinicalOrder("ORD-01", "XQ01", "Chụp X-quang phổi thẳng", "Tư thế đứng thẳng", "COMPLETED"),
                        new VisitSummaryPrintDocument.ClinicalOrder("ORD-02", "XN01", "Tổng phân tích tế bào máu", "Lấy máu lúc sáng", "PENDING")
                ),
                "Nghỉ ngơi tại nhà 3 ngày, uống nhiều nước ấm, tránh dùng đồ lạnh.",
                "Uống thuốc theo đơn chỉ định trong 5 ngày.",
                LocalDate.of(2026, 8, 27),
                "BS. Trần Văn B",
                Instant.parse("2026-08-20T09:15:00Z"),
                "Lễ tân Lê Thị C",
                Instant.parse("2026-08-20T09:30:00Z")
        );

        PdfBoxVisitSummaryPdfRenderer renderer = new PdfBoxVisitSummaryPdfRenderer();
        byte[] pdf = renderer.render(document);

        assertThat(pdf).isNotNull();
        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            assertThat(loaded.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }

        // Verify content formatting: Vietnam timezone (08:30Z -> 15:30 VN), serviceCode, and Vietnamese status
        List<String> lines = renderer.buildLines(document, renderer.createFontMetrics());
        assertThat(lines).anyMatch(l -> l.contains("15:30")); // 08:30Z + 7h = 15:30
        assertThat(lines).anyMatch(l -> l.contains("(Mã: XQ01)"));
        assertThat(lines).anyMatch(l -> l.contains("[Đã có kết quả]"));
        assertThat(lines).anyMatch(l -> l.contains("[Chờ thực hiện]"));
    }

    @Test
    void rendersVisitSummary_withoutOrdersAndRevisitDate() throws Exception {
        VisitSummaryPrintDocument document = new VisitSummaryPrintDocument(
                "Phòng khám A",
                "Hà Nội",
                "0900000000",
                "BN-002",
                "Trần Thị B",
                "1995-10-20",
                "FEMALE",
                "0987654321",
                "KB-002",
                Instant.parse("2026-08-20T08:30:00Z"),
                "BS. C",
                List.of(),
                List.of(),
                null,
                null,
                null,
                "BS. C",
                Instant.parse("2026-08-20T09:00:00Z"),
                "BS. C",
                Instant.parse("2026-08-20T09:05:00Z")
        );

        byte[] pdf = new PdfBoxVisitSummaryPdfRenderer().render(document);

        assertThat(pdf).isNotNull();
        assertThat(pdf).startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));
        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            assertThat(loaded.getNumberOfPages()).isEqualTo(1);
        }
    }
}
