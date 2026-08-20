package com.benhsoan.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.port.dto.result.PrescriptionPrintDocument;

class PdfBoxPrescriptionPdfRendererTest {

    @Test
    void rendersLongClinicAndInstructionTextAcrossMultiplePages() throws Exception {
        String longText = "Hướng dẫn sử dụng thuốc sau bữa ăn và theo dõi phản ứng của cơ thể. ".repeat(12);
        PrescriptionPrintDocument document = new PrescriptionPrintDocument(
                "Phòng khám A", longText, "0900000000", "RX-LONG", UUID.randomUUID(), "PAT-001",
                "Nguyễn Văn A", UUID.randomUUID(), "Bác sĩ B", Instant.parse("2026-08-20T03:00:00Z"),
                java.util.stream.IntStream.range(0, 24)
                        .mapToObj(index -> new PrescriptionPrintDocument.Item(
                                "Thuốc " + index, "500 mg", "viên", "1 viên", 2, 7,
                                AdministrationRoute.ORAL, 14, longText
                        ))
                        .toList()
        );

        byte[] pdf = new PdfBoxPrescriptionPdfRenderer().render(document);

        assertThat(pdf).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            assertThat(loaded.getNumberOfPages()).isGreaterThan(1);
        }
    }
}
