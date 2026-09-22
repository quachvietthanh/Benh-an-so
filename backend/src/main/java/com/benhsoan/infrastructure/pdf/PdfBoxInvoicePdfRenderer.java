package com.benhsoan.infrastructure.pdf;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import com.benhsoan.port.dto.result.billing.InvoicePrintDocument;
import com.benhsoan.port.outbound.pdf.InvoicePdfRenderer;

@Component
public class PdfBoxInvoicePdfRenderer implements InvoicePdfRenderer {

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

    @Override
    public byte[] render(InvoicePrintDocument document) {
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
            throw new PdfRenderingException("Không thể tạo tệp PDF hóa đơn.", ex);
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
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int y = TOP_MARGIN;
        for (String line : lines) {
            graphics.drawString(line, LEFT_MARGIN, y);
            y += LINE_HEIGHT;
        }
        graphics.dispose();
        return image;
    }

    List<String> buildLines(InvoicePrintDocument doc, FontMetrics metrics) {
        List<String> lines = new ArrayList<>();

        String title = "HÓA ĐƠN THU TIỀN KHÁM CHỮA BỆNH";
        if (doc.invoiceType() != null && doc.invoiceType().toUpperCase().contains("ADJUSTMENT")) {
            title = "HÓA ĐƠN ĐIỀU CHỈNH THU TIỀN";
        }
        lines.add(title);
        lines.add("Phòng khám: " + defaultText(doc.clinicName()));
        lines.add("Địa chỉ: " + defaultText(doc.clinicAddress()));
        lines.add("Điện thoại: " + defaultText(doc.clinicPhone()));
        lines.add("");

        lines.add("THÔNG TIN HÓA ĐƠN");
        lines.add("Mã hóa đơn: " + defaultText(doc.invoiceCode()));
        lines.add("Loại hóa đơn: " + ("ADJUSTMENT".equalsIgnoreCase(doc.invoiceType()) ? "Hóa đơn điều chỉnh" : "Hóa đơn gốc"));
        if (doc.originalInvoiceCode() != null && !doc.originalInvoiceCode().isBlank()) {
            lines.add("Mã hóa đơn gốc: " + doc.originalInvoiceCode());
        }
        if (doc.adjustmentReason() != null && !doc.adjustmentReason().isBlank()) {
            lines.add("Lý do điều chỉnh: " + doc.adjustmentReason());
        }
        lines.add("Thời gian lập: " + formatDateTime(doc.createdAt()));
        lines.add("Người lập: " + defaultText(doc.createdByName()));
        lines.add("");

        lines.add("THÔNG TIN BỆNH NHÂN & LƯỢT KHÁM");
        lines.add("Bệnh nhân: " + defaultText(doc.patientName()) + " (Mã BN: " + defaultText(doc.patientCode()) + ")");
        lines.add("Ngày sinh: " + defaultText(doc.patientDateOfBirth()) + " | Giới tính: " + defaultText(doc.patientGender()) + " | SĐT: " + defaultText(doc.patientPhone()));
        lines.add("Mã lượt khám: " + defaultText(doc.visitCode()) + " | Ngày khám: " + formatDateTime(doc.visitAt()));
        lines.add("Bác sĩ phụ trách: " + defaultText(doc.doctorName()) + " | Chuyên khoa: " + defaultText(doc.specialtyName()));
        lines.add("");

        lines.add("DANH MỤC KHOẢN MỤC THU PHÍ");
        if (doc.lines() == null || doc.lines().isEmpty()) {
            lines.add("   (Không có khoản mục nào)");
        } else {
            for (int i = 0; i < doc.lines().size(); i++) {
                var item = doc.lines().get(i);
                lines.add(String.format("%d. %s (%s)", (i + 1), item.itemName(), mapLineType(item.lineType())));
                lines.add(String.format("   Số lượng: %d  |  Đơn giá: %s đ  |  Thành tiền: %s đ",
                        item.quantity(), formatMoney(item.unitPrice()), formatMoney(item.amount())));
            }
        }
        lines.add("");
        lines.add("TỔNG TIỀN THANH TOÁN: " + formatMoney(doc.totalAmount()) + " đ");
        lines.add("");
        lines.add("(Chứng từ điện tử tra cứu từ Cổng bệnh nhân trực tuyến)");

        return lines.stream().flatMap(line -> wrap(line, metrics).stream()).toList();
    }

    List<List<String>> paginate(
            List<String> lines,
            InvoicePrintDocument document,
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

    private List<String> continuationHeader(InvoicePrintDocument document, FontMetrics metrics) {
        return List.of(
                "HÓA ĐƠN THU TIỀN (tiếp theo)",
                "Mã hóa đơn: " + defaultText(document.invoiceCode()),
                "Bệnh nhân: " + defaultText(document.patientName()) + " (" + defaultText(document.patientCode()) + ")",
                ""
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
        for (int index = 0; index < word.length(); index++) {
            currentLine.append(word.charAt(index));
            if (metrics.stringWidth(currentLine.toString()) > CONTENT_WIDTH) {
                char overflow = currentLine.charAt(currentLine.length() - 1);
                currentLine.deleteCharAt(currentLine.length() - 1);
                wrappedLines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(overflow);
            }
        }
    }

    private String formatDateTime(Instant instant) {
        if (instant == null) {
            return "-";
        }
        return DATE_TIME_FORMATTER.format(instant);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amount);
    }

    private String defaultText(String text) {
        return (text == null || text.isBlank()) ? "-" : text.trim();
    }

    private String mapLineType(String lineType) {
        if (lineType == null) {
            return "Chi phí";
        }
        return switch (lineType.toUpperCase()) {
            case "SERVICE" -> "Dịch vụ khám/CLS";
            case "MEDICINE" -> "Thuốc";
            default -> "Khác";
        };
    }
}
