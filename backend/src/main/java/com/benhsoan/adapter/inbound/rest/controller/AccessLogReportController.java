package com.benhsoan.adapter.inbound.rest.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.result.AccessLogReportExportResult;
import com.benhsoan.port.inbound.reporting.ExportAccessLogReportUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reports/access-log")
@RequiredArgsConstructor
@Validated
public class AccessLogReportController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final long MAX_REPORT_RANGE_DAYS = 366;

    private final ExportAccessLogReportUseCase exportAccessLogReportUseCase;

    @GetMapping("/export")
    @RequirePermission("ACCESS_LOG_REPORT_EXPORT")
    public ResponseEntity<ByteArrayResource> export(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        validateRange(fromDate, toDate);

        AccessLogReportExportResult result = exportAccessLogReportUseCase.export(fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.content().length)
                .body(new ByteArrayResource(result.content()));
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
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
        long inclusiveDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (inclusiveDays > MAX_REPORT_RANGE_DAYS) {
            throw new ValidationException("Date range must not exceed 366 days.");
        }
    }
}
