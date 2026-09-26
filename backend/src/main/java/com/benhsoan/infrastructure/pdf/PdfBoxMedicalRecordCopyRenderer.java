package com.benhsoan.infrastructure.pdf;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
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

import com.benhsoan.port.dto.result.MedicalRecordCopyDocument;
import com.benhsoan.port.outbound.pdf.MedicalRecordCopyPdfRenderer;

@Component
public class PdfBoxMedicalRecordCopyRenderer implements MedicalRecordCopyPdfRenderer {

    private static final int IMAGE_WIDTH = 1190;
    private static final int IMAGE_HEIGHT = 1684;
    private static final int LEFT_MARGIN = 100;
    private static final int TOP_MARGIN = 110;
    private static final int LINE_HEIGHT = 32;
    private static final int LINES_PER_PAGE = 48;
    private static final int CONTENT_WIDTH = IMAGE_WIDTH - (LEFT_MARGIN * 2);
    private static final Font CONTENT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 22);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);

    @Override
    public byte[] render(MedicalRecordCopyDocument document) {
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
            throw new PdfRenderingException("Unable to generate medical record copy PDF.", ex);
        }
    }

    private FontMetrics createFontMetrics() {
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

    private List<String> buildLines(MedicalRecordCopyDocument d, FontMetrics metrics) {
        List<String> lines = new ArrayList<>();
        lines.add("BẢN SAO HỒ SƠ BỆNH ÁN");
        lines.add("");
        lines.add("PHÒNG KHÁM: " + nvl(d.clinicName()));
        lines.add("Địa chỉ: " + nvl(d.clinicAddress()));
        lines.add("Điện thoại: " + nvl(d.clinicPhone()));
        lines.add("");
        lines.add("Mã bệnh nhân: " + nvl(d.patientCode()));
        lines.add("Họ tên: " + nvl(d.patientName()));
        lines.add("Ngày sinh: " + nvl(d.patientDateOfBirth()));
        lines.add("Giới tính: " + nvl(d.patientGender()));
        lines.add("Mã lượt khám: " + nvl(d.visitCode()));
        lines.add("Thời gian khám: " + (d.visitAt() == null ? "-" : DATE_TIME_FORMATTER.format(d.visitAt())));
        lines.add("Bác sĩ ký: " + nvl(d.doctorName()));
        lines.add("");
        lines.add("Lý do khám: " + nvl(d.chiefComplaint()));
        lines.add("Triệu chứng: " + nvl(d.symptoms()));
        lines.add("Tiền sử bệnh: " + nvl(d.medicalHistory()));
        lines.add("Khám lâm sàng: " + nvl(d.physicalExamination()));
        lines.add("Diễn tiến: " + nvl(d.clinicalProgress()));
        lines.add("Hướng điều trị: " + nvl(d.treatmentPlan()));
        lines.add("Chỉ định của bác sĩ: " + nvl(d.doctorInstructions()));
        lines.add("Kết luận: " + nvl(d.conclusion()));
        lines.add("");
        lines.add("CHẨN ĐOÁN:");
        for (MedicalRecordCopyDocument.Diagnosis diagnosis : d.diagnoses()) {
            lines.add("- " + nvl(diagnosis.code()) + " " + nvl(diagnosis.name()));
        }
        return lines.stream().flatMap(line -> wrap(line, metrics).stream()).toList();
    }

    private List<List<String>> paginate(
            List<String> lines,
            MedicalRecordCopyDocument document,
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

    private List<String> continuationHeader(MedicalRecordCopyDocument document, FontMetrics metrics) {
        return List.of(
                "BẢN SAO HỒ SƠ BỆNH ÁN (tiếp theo)",
                "Bệnh nhân: " + nvl(document.patientName()) + " (" + nvl(document.patientCode()) + ")",
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

    private void addLongWord(String word, FontMetrics metrics, List<String> wrappedLines, StringBuilder currentLine) {
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

    private String nvl(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}

