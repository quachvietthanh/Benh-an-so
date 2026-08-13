package com.benhsoan.adapter.inbound.rest.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.response.reporting.OperationalSummaryResponse;
import com.benhsoan.adapter.inbound.rest.response.reporting.OperationalTimelineItemResponse;
import com.benhsoan.adapter.inbound.rest.response.reporting.OperationalTimelineResponse;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Validated
public class ReportsController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OperationalSummaryResponse getSummary(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        validateRange(fromDate, toDate);

        return new OperationalSummaryResponse(
                fromDate,
                toDate,
                0L,
                BigDecimal.ZERO,
                "VND"
        );
    }

    @GetMapping("/visits-timeline")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OperationalTimelineResponse getVisitsTimeline(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        validateRange(fromDate, toDate);

        return new OperationalTimelineResponse(fromDate, toDate, buildEmptyTimeline(fromDate, toDate));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ByteArrayResource> export(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        validateRange(fromDate, toDate);

        OperationalSummaryResponse summary = new OperationalSummaryResponse(
                fromDate,
                toDate,
                0L,
                BigDecimal.ZERO,
                "VND"
        );
        List<OperationalTimelineItemResponse> timeline = buildEmptyTimeline(fromDate, toDate);
        byte[] content = buildCsv(summary, timeline);
        String filename = "operational-report-" + fromDate + "-to-" + toDate + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(CSV_CONTENT_TYPE))
                .contentLength(content.length)
                .body(new ByteArrayResource(content));
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (RuntimeException ex) {
            throw new ValidationException(fieldName + " must be in yyyy-MM-dd format.");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ValidationException("from must be before or equal to to.");
        }
    }

    private List<OperationalTimelineItemResponse> buildEmptyTimeline(LocalDate from, LocalDate to) {
        List<OperationalTimelineItemResponse> items = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            items.add(new OperationalTimelineItemResponse(current, 0L, BigDecimal.ZERO));
            current = current.plusDays(1);
        }
        return items;
    }

    private byte[] buildCsv(
            OperationalSummaryResponse summary,
            List<OperationalTimelineItemResponse> timeline
    ) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("OPERATIONAL REPORT\n");
        csv.append("From,").append(summary.from()).append('\n');
        csv.append("To,").append(summary.to()).append('\n');
        csv.append("Visit Count,").append(summary.visitCount()).append('\n');
        csv.append("Revenue (").append(summary.currency()).append("),").append(summary.revenue()).append("\n\n");
        csv.append("Date,Visit Count,Revenue (").append(summary.currency()).append(")\n");

        for (OperationalTimelineItemResponse item : timeline) {
            csv.append(item.date()).append(',')
                    .append(item.visitCount()).append(',')
                    .append(item.revenue()).append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
