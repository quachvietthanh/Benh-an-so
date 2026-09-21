package com.benhsoan.adapter.inbound.rest.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.InventoryRestMapper;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryBatchResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryExpiryAlertResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryStockReportResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.InventoryStockResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.LowStockMedicineResponse;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.inventory.enums.InventoryExpiryAlertStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.query.inventory.ListInventoryBatchesQuery;
import com.benhsoan.port.dto.query.inventory.ListInventoryExpiryAlertsQuery;
import com.benhsoan.port.dto.query.inventory.ListInventoryStocksQuery;
import com.benhsoan.port.inbound.inventory.GetInventoryStockReportUseCase;
import com.benhsoan.port.inbound.inventory.ListInventoryExpiryAlertsUseCase;
import com.benhsoan.port.inbound.inventory.ListInventoryBatchesUseCase;
import com.benhsoan.port.inbound.inventory.ListLowStockMedicinesUseCase;
import com.benhsoan.port.inbound.inventory.ListInventoryStocksUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final long MAX_REPORT_RANGE_DAYS = 366;

    private final ListInventoryStocksUseCase listInventoryStocksUseCase;
    private final ListInventoryBatchesUseCase listInventoryBatchesUseCase;
    private final ListInventoryExpiryAlertsUseCase listInventoryExpiryAlertsUseCase;
    private final ListLowStockMedicinesUseCase listLowStockMedicinesUseCase;
    private final GetInventoryStockReportUseCase getInventoryStockReportUseCase;
    private final InventoryRestMapper mapper;

    @GetMapping("/stocks")
    @RequirePermission("PHARMACY_READ")
    public List<InventoryStockResponse> listStocks(
            @RequestParam(required = false) Boolean active
    ) {
        return mapper.toStockResponses(
                listInventoryStocksUseCase.list(new ListInventoryStocksQuery(active))
        );
    }

    @GetMapping("/batches")
    @RequirePermission("PHARMACY_READ")
    public List<InventoryBatchResponse> listBatches(
            @RequestParam(required = false) UUID medicineId,
            @RequestParam(required = false) BatchStatus status,
            @RequestParam(required = false) Boolean eligibleForDispense
    ) {
        return mapper.toBatchResponses(
                listInventoryBatchesUseCase.list(
                        new ListInventoryBatchesQuery(medicineId, status, eligibleForDispense)
                )
        );
    }

    @GetMapping("/low-stock")
    @RequirePermission("PHARMACY_READ")
    public List<LowStockMedicineResponse> listLowStockMedicines() {
        return mapper.toLowStockResponses(
                listLowStockMedicinesUseCase.list()
        );
    }

    @GetMapping("/expiry-alerts")
    @RequirePermission("PHARMACY_READ")
    public List<InventoryExpiryAlertResponse> listExpiryAlerts(
            @RequestParam(required = false) UUID medicineId,
            @RequestParam(required = false) InventoryExpiryAlertStatus status
    ) {
        return mapper.toExpiryAlertResponses(
                listInventoryExpiryAlertsUseCase.list(
                        new ListInventoryExpiryAlertsQuery(medicineId, status)
                )
        );
    }

    @GetMapping("/report/stock-in-out")
    @RequirePermission({"PHARMACY_READ", "REPORT_VIEW"})
    public InventoryStockReportResponse getStockReport(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        validateRange(fromDate, toDate);

        return mapper.toStockReportResponse(
                getInventoryStockReportUseCase.getStockReport(fromDate, toDate)
        );
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
