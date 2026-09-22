package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.port.dto.result.InventoryStockReportExportResult;
import com.benhsoan.port.dto.result.InventoryStockReportItemResult;
import com.benhsoan.port.dto.result.InventoryStockReportResult;
import com.benhsoan.port.inbound.inventory.GetInventoryStockReportUseCase;

class ExportInventoryStockReportServiceTest {

    private final GetInventoryStockReportUseCase getInventoryStockReportUseCase =
            mock(GetInventoryStockReportUseCase.class);

    private final ExportInventoryStockReportService service =
            new ExportInventoryStockReportService(getInventoryStockReportUseCase);

    @Test
    void exportContainsRequiredColumnsAndMatchingValues() {
        UUID medicineId = UUID.randomUUID();
        when(getInventoryStockReportUseCase.getStockReport(any(), any()))
                .thenReturn(new InventoryStockReportResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        Instant.parse("2026-08-31T08:00:00Z"),
                        true,
                        List.of(new InventoryStockReportItemResult(
                                medicineId, "MED-001", "Paracetamol", "vien",
                                100, 50, 30, 5, -10, 115
                        ))
                ));

        InventoryStockReportExportResult result = service.export(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        String csv = new String(result.content(), StandardCharsets.UTF_8);

        assertEquals("stock-in-out-report-2026-08-01-to-2026-08-31.csv", result.fileName());
        assertEquals("text/csv; charset=UTF-8", result.contentType());
        assertTrue(csv.contains("Medicine ID"));
        assertTrue(csv.contains("Opening Quantity"));
        assertTrue(csv.contains("Received Quantity"));
        assertTrue(csv.contains("Dispensed Quantity"));
        assertTrue(csv.contains("Returned Quantity"));
        assertTrue(csv.contains("Adjusted Quantity"));
        assertTrue(csv.contains("Closing Quantity"));
        assertTrue(csv.contains(medicineId.toString()));
        assertTrue(csv.contains("MED-001"));
        assertTrue(csv.contains("Paracetamol"));
        assertTrue(csv.contains("vien"));
        assertTrue(csv.contains("100,50,30,5,-10,115"));
    }

    @Test
    void exportHandlesEmptyPeriodWithHeaderOnly() {
        when(getInventoryStockReportUseCase.getStockReport(any(), any()))
                .thenReturn(new InventoryStockReportResult(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        Instant.parse("2026-08-31T08:00:00Z"),
                        false,
                        List.of()
                ));

        InventoryStockReportExportResult result = service.export(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        String csv = new String(result.content(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("Closing Quantity"));
        assertEquals(1, csv.lines().count());
    }
}
