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

import com.benhsoan.port.dto.result.PrescriptionPrintDocument;
import com.benhsoan.port.outbound.pdf.PrescriptionPdfRenderer;

@Component
public class PdfBoxPrescriptionPdfRenderer implements PrescriptionPdfRenderer {

    private static final int IMAGE_WIDTH = 1190;
    private static final int IMAGE_HEIGHT = 1684;
    private static final int LEFT_MARGIN = 100;
    private static final int TOP_MARGIN = 110;
    private static final int LINE_HEIGHT = 32;
    private static final int LINES_PER_PAGE = 48;
    private static final int CONTENT_WIDTH = IMAGE_WIDTH - (LEFT_MARGIN * 2);
    private static final Font CONTENT_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 22);

    @Override
    public byte[] render(PrescriptionPrintDocument prescription) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            FontMetrics metrics = createFontMetrics();
            List<List<String>> pages = paginate(buildLines(prescription, metrics), prescription, metrics);
            for (List<String> page : pages) {
                BufferedImage pageImage = renderPage(page);
                document.addPage(new PDPage(PDRectangle.A4));
                PDImageXObject image = LosslessFactory.createFromImage(document, pageImage);
                try (PDPageContentStream content = new PDPageContentStream(
                        document, document.getPage(document.getNumberOfPages() - 1))) {
                    content.drawImage(image, 0, 0, PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight());
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new PdfRenderingException("Unable to generate prescription PDF.", ex);
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

    private List<String> buildLines(PrescriptionPrintDocument prescription, FontMetrics metrics) {
        List<String> lines = new ArrayList<>();
        lines.add("PRESCRIPTION");
        lines.add("Clinic: " + prescription.clinicName());
        lines.add("Address: " + prescription.clinicAddress());
        lines.add("Phone: " + prescription.clinicPhone());
        lines.add("");
        lines.add("Prescription code: " + prescription.prescriptionCode());
        lines.add("Patient: " + prescription.patientName() + " (" + prescription.patientCode() + ")");
        lines.add("Doctor: " + prescription.doctorName());
        lines.add("Prescribed at: " + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                prescription.prescribedAt().atOffset(ZoneOffset.UTC)));
        lines.add("");
        lines.add("MEDICINES");
        for (int index = 0; index < prescription.items().size(); index++) {
            PrescriptionPrintDocument.Item item = prescription.items().get(index);
            lines.add((index + 1) + ". " + item.medicineName() + " - " + item.strength() + " " + item.unit());
            lines.add("   Dose: " + item.dosage() + "; Frequency: " + item.frequency()
                    + "/day; Duration: " + item.durationDays() + " days; Route: " + item.route());
            lines.add("   Quantity: " + item.quantity() + "; Instructions: "
                    + (item.instructions() == null ? "-" : item.instructions()));
        }
        return lines.stream().flatMap(line -> wrap(line, metrics).stream()).toList();
    }

    private List<List<String>> paginate(
            List<String> lines,
            PrescriptionPrintDocument prescription,
            FontMetrics metrics
    ) {
        List<List<String>> pages = new ArrayList<>();
        int nextLine = 0;
        while (nextLine < lines.size()) {
            List<String> page = new ArrayList<>();
            if (!pages.isEmpty()) {
                List<String> header = continuationHeader(prescription, metrics);
                page.addAll(header.subList(0, Math.min(header.size(), LINES_PER_PAGE - 1)));
            }
            while (nextLine < lines.size() && page.size() < LINES_PER_PAGE) {
                page.add(lines.get(nextLine++));
            }
            pages.add(page);
        }
        return pages;
    }

    private List<String> continuationHeader(PrescriptionPrintDocument prescription, FontMetrics metrics) {
        return List.of(
                "PRESCRIPTION (continued)",
                "Prescription code: " + prescription.prescriptionCode(),
                "Patient: " + prescription.patientName() + " (" + prescription.patientCode() + ")",
                "Doctor: " + prescription.doctorName(),
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
}
