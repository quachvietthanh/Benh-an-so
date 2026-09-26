package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.port.dto.result.InventoryStockReportResult;
import com.benhsoan.port.outbound.repository.inventory.InventoryStockMovementSummary;
import com.benhsoan.port.outbound.repository.inventory.InventoryStockReportRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.time.ClockPort;

class GetInventoryStockReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-03T08:00:00Z");

    private final InventoryStockReportRepository reportRepository = mock(InventoryStockReportRepository.class);
    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private final GetInventoryStockReportService service =
            new GetInventoryStockReportService(reportRepository, medicineRepository, clockPort);

    @Test
    void computesClosingStockUsingReconciliationFormula() {
        UUID medicineId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(reportRepository.summarizeMovements(any(), any())).thenReturn(List.of(
                new InventoryStockMovementSummary(medicineId, 100, 20, 30, 5, -2)
        ));
        when(medicineRepository.findAllById(anyCollection())).thenReturn(List.of(
                medicine(medicineId, "MED-001", "Paracetamol", "vien")
        ));

        InventoryStockReportResult result = service.getStockReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertEquals(1, result.items().size());
        var item = result.items().get(0);
        assertEquals(100, item.openingQuantity());
        assertEquals(20, item.receivedQuantity());
        assertEquals(30, item.dispensedQuantity());
        assertEquals(5, item.returnedQuantity());
        assertEquals(-2, item.adjustedQuantity());
        assertEquals(93, item.closingQuantity());
        assertEquals("MED-001", item.medicineCode());
        assertEquals("Paracetamol", item.medicineName());
        assertEquals("vien", item.unit());
        assertTrue(result.hasTransactions());
        assertEquals(NOW, result.generatedAt());
    }

    @Test
    void aggregatesEachMedicineIndependently() {
        UUID medA = UUID.randomUUID();
        UUID medB = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(reportRepository.summarizeMovements(any(), any())).thenReturn(List.of(
                new InventoryStockMovementSummary(medA, 10, 5, 0, 0, 0),
                new InventoryStockMovementSummary(medB, 0, 0, 8, 0, 0)
        ));
        when(medicineRepository.findAllById(anyCollection())).thenReturn(List.of(
                medicine(medA, "A", "Medicine A", "vien"),
                medicine(medB, "B", "Medicine B", "vien")
        ));

        InventoryStockReportResult result = service.getStockReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertEquals(2, result.items().size());
        var a = result.items().get(0);
        var b = result.items().get(1);
        assertEquals(15, a.closingQuantity());
        assertEquals(-8, b.closingQuantity());
        assertTrue(result.hasTransactions());
    }

    @Test
    void emptyPeriodKeepsOpeningEqualToClosingAndReportsNoTransactions() {
        UUID medicineId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(reportRepository.summarizeMovements(any(), any())).thenReturn(List.of(
                new InventoryStockMovementSummary(medicineId, 40, 0, 0, 0, 0)
        ));
        when(medicineRepository.findAllById(anyCollection())).thenReturn(List.of(
                medicine(medicineId, "MED-001", "Paracetamol", "vien")
        ));

        InventoryStockReportResult result = service.getStockReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertEquals(1, result.items().size());
        var item = result.items().get(0);
        assertEquals(40, item.openingQuantity());
        assertEquals(0, item.receivedQuantity());
        assertEquals(0, item.dispensedQuantity());
        assertEquals(0, item.returnedQuantity());
        assertEquals(0, item.adjustedQuantity());
        assertEquals(40, item.closingQuantity());
        assertFalse(result.hasTransactions());
    }

    @Test
    void emptyMovementListYieldsEmptyItemsAndNoTransactions() {
        when(clockPort.now()).thenReturn(NOW);
        when(reportRepository.summarizeMovements(any(), any())).thenReturn(List.of());

        InventoryStockReportResult result = service.getStockReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertTrue(result.items().isEmpty());
        assertFalse(result.hasTransactions());
    }

    @Test
    void convertsPeriodBoundariesUsingClinicTimezone() {
        when(reportRepository.summarizeMovements(any(), any())).thenReturn(List.of());

        service.getStockReport(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1));

        verify(reportRepository).summarizeMovements(
                eq(Instant.parse("2026-07-31T17:00:00Z")),
                eq(Instant.parse("2026-08-01T17:00:00Z"))
        );
    }

    @Test
    void computesExplicitReconciliationFormula() {
        UUID medicineId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(reportRepository.summarizeMovements(any(), any())).thenReturn(List.of(
                new InventoryStockMovementSummary(medicineId, 100, 50, 30, 5, -10)
        ));
        when(medicineRepository.findAllById(anyCollection())).thenReturn(List.of(
                medicine(medicineId, "MED-001", "Paracetamol", "vien")
        ));

        InventoryStockReportResult result = service.getStockReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertEquals(115, result.items().get(0).closingQuantity());
    }

    @Test
    void filtersOutZeroOnlyRows() {
        UUID medicineId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(reportRepository.summarizeMovements(any(), any())).thenReturn(List.of(
                new InventoryStockMovementSummary(medicineId, 0, 0, 0, 0, 0)
        ));

        InventoryStockReportResult result = service.getStockReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertTrue(result.items().isEmpty());
        assertFalse(result.hasTransactions());
    }

    @Test
    void keepsMedicineWithOpeningButNoInPeriodMovement() {
        UUID medicineId = UUID.randomUUID();
        when(clockPort.now()).thenReturn(NOW);
        when(reportRepository.summarizeMovements(any(), any())).thenReturn(List.of(
                new InventoryStockMovementSummary(medicineId, 40, 0, 0, 0, 0)
        ));
        when(medicineRepository.findAllById(anyCollection())).thenReturn(List.of(
                medicine(medicineId, "MED-001", "Paracetamol", "vien")
        ));

        InventoryStockReportResult result = service.getStockReport(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertEquals(1, result.items().size());
        assertEquals(40, result.items().get(0).openingQuantity());
        assertEquals(40, result.items().get(0).closingQuantity());
        assertFalse(result.hasTransactions());
    }

    private Medicine medicine(UUID id, String code, String name, String unit) {
        return Medicine.restore(
                id,
                code,
                name,
                "paracetamol",
                "500mg",
                DosageForm.TABLET,
                unit,
                AdministrationRoute.ORAL,
                true,
                NOW,
                null,
                100,
                10
        );
    }
}
