package com.benhsoan.adapter.inbound.rest.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

import com.benhsoan.adapter.inbound.rest.mapper.ReportingRestMapper;
import com.benhsoan.adapter.inbound.rest.response.reporting.OperationalSummaryResponse;
import com.benhsoan.adapter.inbound.rest.response.reporting.OperationalTimelineResponse;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.OperationalReportExportResult;
import com.benhsoan.port.inbound.reporting.ExportOperationalReportUseCase;
import com.benhsoan.port.inbound.reporting.GetOperationalSummaryUseCase;
import com.benhsoan.port.inbound.reporting.GetOperationalTimelineUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Validated
public class ReportsController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final GetOperationalSummaryUseCase getOperationalSummaryUseCase;
    private final GetOperationalTimelineUseCase getOperationalTimelineUseCase;
    private final ExportOperationalReportUseCase exportOperationalReportUseCase;
    private final ReportingRestMapper mapper;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OperationalSummaryResponse getSummary(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        validateRange(fromDate, toDate);

        return mapper.toResponse(getOperationalSummaryUseCase.getSummary(fromDate, toDate));
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

        return mapper.toResponse(getOperationalTimelineUseCase.getTimeline(fromDate, toDate));
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

        OperationalReportExportResult exportResult = exportOperationalReportUseCase.export(fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportResult.fileName() + "\"")
                .contentType(MediaType.parseMediaType(exportResult.contentType()))
                .contentLength(exportResult.content().length)
                .body(new ByteArrayResource(exportResult.content()));
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
}
