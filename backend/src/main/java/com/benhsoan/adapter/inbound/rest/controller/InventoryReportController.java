package com.benhsoan.adapter.inbound.rest.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.InventoryReportRestMapper;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryInOutStockReportResponse;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.query.inventory.GetInventoryInOutStockReportQuery;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockExportResult;
import com.benhsoan.port.inbound.inventory.ExportInventoryInOutStockReportUseCase;
import com.benhsoan.port.inbound.inventory.GetInventoryInOutStockReportUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventory/reports/in-out-stock")
@RequiredArgsConstructor
@Validated
public class InventoryReportController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final long MAX_REPORT_RANGE_DAYS = 366;

    private final GetInventoryInOutStockReportUseCase getInventoryInOutStockReportUseCase;
    private final ExportInventoryInOutStockReportUseCase exportInventoryInOutStockReportUseCase;
    private final InventoryReportRestMapper mapper;

    @GetMapping
    @RequirePermission(value = {"PHARMACY_READ", "REPORT_VIEW"}, operator = RequirePermission.Operator.ANY)
    public InventoryInOutStockReportResponse getReport(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) UUID medicineId,
            @RequestParam(required = false) String keyword
    ) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        validateRange(fromDate, toDate);

        GetInventoryInOutStockReportQuery query = new GetInventoryInOutStockReportQuery(
                fromDate,
                toDate,
                medicineId,
                keyword
        );

        return mapper.toResponse(getInventoryInOutStockReportUseCase.getReport(query));
    }

    @GetMapping("/export")
    @RequirePermission(value = {"PHARMACY_READ", "REPORT_EXPORT"}, operator = RequirePermission.Operator.ANY)
    public ResponseEntity<ByteArrayResource> export(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) UUID medicineId,
            @RequestParam(required = false) String keyword
    ) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        validateRange(fromDate, toDate);

        GetInventoryInOutStockReportQuery query = new GetInventoryInOutStockReportQuery(
                fromDate,
                toDate,
                medicineId,
                keyword
        );

        InventoryInOutStockExportResult exportResult = exportInventoryInOutStockReportUseCase.export(query);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportResult.fileName() + "\"")
                .contentType(MediaType.parseMediaType(exportResult.contentType()))
                .contentLength(exportResult.content().length)
                .body(new ByteArrayResource(exportResult.content()));
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
            throw new ValidationException("Date range must not exceed " + MAX_REPORT_RANGE_DAYS + " days.");
        }
    }
}
