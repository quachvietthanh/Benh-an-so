package com.benhsoan.infrastructure.pdf;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import com.benhsoan.port.dto.result.VisitSummaryPrintDocument;
import com.benhsoan.port.outbound.pdf.VisitSummaryPdfRenderer;

@Component
public class PdfBoxVisitSummaryPdfRenderer implements VisitSummaryPdfRenderer {

    private static final int IMAGE_WIDTH = 1190;
    private static final int IMAGE_HEIGHT = 1684;
    private static final int LEFT_MARGIN = 100;
    private static final int TOP_MARGIN = 110;
    private static final int LINE_HEIGHT = 32;
    private static final int LINES_PER_PAGE = 48;
    private static final int CONTENT_WIDTH = IMAGE_WIDTH - (LEFT_MARGIN * 2);
    private static final Font CONTENT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 22);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(VIETNAM_ZONE);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public byte[] render(VisitSummaryPrintDocument document) {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            FontMetrics metrics = createFontMetrics();
            List<List<String>> pages = paginate(buildLines(document, metrics), document, metrics);
            for (List<String> page : pages) {
                BufferedImage pageImage = renderPage(page);
                pdf.addPage(new PDPage(PDRectangle.A4));
                PDImageXObject image = LosslessFactory.createFromImage(pdf, pageImage);
                try (PDPageContentStream content = new PDPageContentStream(
                        pdf, pdf.getPage(pdf.getNumberOfPages() - 1))) {
                    content.drawImage(image, 0, 0, PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight());
                }
            }
            pdf.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new PdfRenderingException("Unable to generate visit summary PDF.", ex);
        }
    }

    FontMetrics createFontMetrics() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            return graphics.getFontMetrics(CONTENT_FONT);
        } finally {
            graphics.dispose();
        }
    }

    private BufferedImage renderPage(List<String> lines) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        graphics.setColor(Color.BLACK);
        graphics.setFont(CONTENT_FONT);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int y = TOP_MARGIN;
        for (String line : lines) {
            graphics.drawString(line, LEFT_MARGIN, y);
            y += LINE_HEIGHT;
        }
        graphics.dispose();
        return image;
    }

    List<String> buildLines(VisitSummaryPrintDocument d, FontMetrics metrics) {
        List<String> lines = new ArrayList<>();
        lines.add("PHIẾU TÓM TẮT LƯỢT KHÁM");
        lines.add("================================================================================");
        lines.add("PHÒNG KHÁM: " + nvl(d.clinicName()));
        lines.add("Địa chỉ: " + nvl(d.clinicAddress()));
        lines.add("Điện thoại: " + nvl(d.clinicPhone()));
        lines.add("");
        lines.add("1. THÔNG TIN BỆNH NHÂN");
        lines.add("- Mã bệnh nhân: " + nvl(d.patientCode()) + "  |  Họ và tên: " + nvl(d.patientName()));
        lines.add("- Ngày sinh: " + nvl(d.patientDateOfBirth()) + "  |  Giới tính: " + nvl(d.patientGender()) + "  |  SĐT: " + nvl(d.patientPhone()));
        lines.add("");
        lines.add("2. THÔNG TIN LƯỢT KHÁM");
        lines.add("- Mã lượt khám: " + nvl(d.visitCode()));
        lines.add("- Thời gian khám: " + (d.visitAt() == null ? "-" : DATE_TIME_FORMATTER.format(d.visitAt())));
        lines.add("- Bác sĩ khám: " + nvl(d.doctorName()));
        lines.add("");
        lines.add("3. CHẨN ĐOÁN");
        if (d.diagnoses() == null || d.diagnoses().isEmpty()) {
            lines.add("- Chưa ghi nhận chẩn đoán");
        } else {
            for (VisitSummaryPrintDocument.Diagnosis diagnosis : d.diagnoses()) {
                String type = diagnosis.isPrimary() ? "(Chẩn đoán chính)" : "(Chẩn đoán kèm theo)";
                lines.add("- [" + nvl(diagnosis.code()) + "] " + nvl(diagnosis.name()) + " " + type);
            }
        }
        lines.add("");
        lines.add("4. CHỈ ĐỊNH CẬN LÂM SÀNG");
        if (d.clinicalOrders() == null || d.clinicalOrders().isEmpty()) {
            lines.add("- Không có chỉ định cận lâm sàng trong lượt khám");
        } else {
            int idx = 1;
            for (VisitSummaryPrintDocument.ClinicalOrder order : d.clinicalOrders()) {
                String instruction = (order.instruction() != null && !order.instruction().isBlank())
                        ? " - Ghi chú: " + order.instruction() : "";
                String status = formatOrderStatus(order.status());
                String codeStr = (order.serviceCode() != null && !order.serviceCode().isBlank())
                        ? " (Mã: " + order.serviceCode() + ")" : "";
                lines.add(idx++ + ". " + nvl(order.serviceName()) + codeStr + instruction + status);
            }
        }
        lines.add("");
        lines.add("5. KẾ HOẠCH ĐIỀU TRỊ & LỜI DẶN CỦA BÁC SĨ");
        lines.add("- Hướng điều trị: " + (d.treatmentPlan() != null && !d.treatmentPlan().isBlank() ? d.treatmentPlan() : "Theo dõi ngoại trú"));
        lines.add("- Lời dặn: " + (d.doctorInstructions() != null && !d.doctorInstructions().isBlank() ? d.doctorInstructions() : "Tuân thủ hướng dẫn dùng thuốc và tái khám đúng hẹn"));
        lines.add("");
        lines.add("6. HẸN TÁI KHÁM");
        lines.add("- Mốc tái khám: " + formatRevisitDate(d.revisitDate()));
        lines.add("");
        lines.add("================================================================================");
        lines.add("BÁC SĨ PHỤ TRÁCH LƯỢT KHÁM: " + nvl(d.signedByName()) + " (Đã ký xác nhận điện tử: "
                + (d.signedAt() == null ? "-" : DATE_TIME_FORMATTER.format(d.signedAt())) + ")");
        lines.add("Người in: " + nvl(d.printedByName()) + "  |  Thời điểm in: "
                + (d.printedAt() == null ? "-" : DATE_TIME_FORMATTER.format(d.printedAt())));

        return lines.stream().flatMap(line -> wrap(line, metrics).stream()).toList();
    }

    private String formatOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        if ("COMPLETED".equalsIgnoreCase(status.trim())) {
            return " [Đã có kết quả]";
        }
        if ("PENDING".equalsIgnoreCase(status.trim())) {
            return " [Chờ thực hiện]";
        }
        return " [" + status + "]";
    }

    private String formatRevisitDate(LocalDate revisitDate) {
        if (revisitDate == null) {
            return "Tái khám khi có dấu hiệu bất thường hoặc theo hướng dẫn của bác sĩ.";
        }
        return DATE_FORMATTER.format(revisitDate);
    }

    private List<List<String>> paginate(
            List<String> lines,
            VisitSummaryPrintDocument document,
            FontMetrics metrics
    ) {
        List<List<String>> pages = new ArrayList<>();
        int nextLine = 0;
        while (nextLine < lines.size()) {
            List<String> page = new ArrayList<>();
            if (!pages.isEmpty()) {
                List<String> header = continuationHeader(document, metrics);
                page.addAll(header.subList(0, Math.min(header.size(), LINES_PER_PAGE - 1)));
            }
            while (nextLine < lines.size() && page.size() < LINES_PER_PAGE) {
                page.add(lines.get(nextLine++));
            }
            pages.add(page);
        }
        return pages;
    }

    private List<String> continuationHeader(VisitSummaryPrintDocument document, FontMetrics metrics) {
        return List.of(
                "PHIẾU TÓM TẮT LƯỢT KHÁM (tiếp theo)",
                "Bệnh nhân: " + nvl(document.patientName()) + " (" + nvl(document.patientCode()) + ")",
                "--------------------------------------------------------------------------------"
        ).stream().flatMap(line -> wrap(line, metrics).stream()).toList();
    }

    private List<String> wrap(String line, FontMetrics metrics) {
        if (line.isEmpty()) {
            return List.of("");
        }
        List<String> wrappedLines = new ArrayList<>();
        for (String paragraph : line.split("\\R", -1)) {
            wrapParagraph(paragraph, metrics, wrappedLines);
        }
        return wrappedLines;
    }

    private void wrapParagraph(String paragraph, FontMetrics metrics, List<String> wrappedLines) {
        if (paragraph.isEmpty()) {
            wrappedLines.add("");
            return;
        }
        StringBuilder currentLine = new StringBuilder();
        for (String word : paragraph.split(" ")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (metrics.stringWidth(candidate) <= CONTENT_WIDTH) {
                currentLine.setLength(0);
                currentLine.append(candidate);
                continue;
            }
            if (!currentLine.isEmpty()) {
                wrappedLines.add(currentLine.toString());
                currentLine.setLength(0);
            }
            addLongWord(word, metrics, wrappedLines, currentLine);
        }
        if (!currentLine.isEmpty()) {
            wrappedLines.add(currentLine.toString());
        }
    }

    private void addLongWord(
            String word,
            FontMetrics metrics,
            List<String> wrappedLines,
            StringBuilder currentLine
    ) {
        StringBuilder chunk = new StringBuilder();
        for (char ch : word.toCharArray()) {
            String candidate = chunk.toString() + ch;
            if (metrics.stringWidth(candidate) > CONTENT_WIDTH && !chunk.isEmpty()) {
                wrappedLines.add(chunk.toString());
                chunk.setLength(0);
            }
            chunk.append(ch);
        }
        currentLine.append(chunk);
    }

    private String nvl(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
