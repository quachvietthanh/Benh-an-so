package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.inventory.enums.StockMovementType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.query.inventory.GetInventoryInOutStockReportQuery;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockItemResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockReportResult;
import com.benhsoan.port.dto.result.inventory.MedicineMovementSummaryResult;
import com.benhsoan.port.dto.result.inventory.MedicineStockQuantityResult;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.time.ClockPort;

class GetInventoryInOutStockReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:00:00Z");

    private final StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final InventoryReportAuthorizer authorizer = mock(InventoryReportAuthorizer.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private GetInventoryInOutStockReportService service;

    @BeforeEach
    void setUp() {
        when(clockPort.now()).thenReturn(NOW);
        service = new GetInventoryInOutStockReportService(
                stockMovementRepository,
                medicineRepository,
                authorizer,
                clockPort
        );
    }

    @Test
    void successfullyCalculatesInOutStockReport_withCompleteTransactions() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        UUID medId = UUID.randomUUID();
        Medicine medicine = createMedicine(medId, "MED-001", "Paracetamol 500mg", "Viên");

        when(medicineRepository.findAll()).thenReturn(List.of(medicine));

        when(stockMovementRepository.sumQuantitiesBefore(any(Instant.class)))
                .thenReturn(List.of(new MedicineStockQuantityResult(medId, 50L)));

        when(stockMovementRepository.sumMovementsBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        new MedicineMovementSummaryResult(medId, StockMovementType.RECEIPT, 100L),
                        new MedicineMovementSummaryResult(medId, StockMovementType.DISPENSE, -30L),
                        new MedicineMovementSummaryResult(medId, StockMovementType.RETURN, 10L),
                        new MedicineMovementSummaryResult(medId, StockMovementType.ADJUSTMENT, -3L),
                        new MedicineMovementSummaryResult(medId, StockMovementType.EXPIRE, -2L)
                ));

        GetInventoryInOutStockReportQuery query = new GetInventoryInOutStockReportQuery(from, to, null, null);
        InventoryInOutStockReportResult result = service.getReport(query);

        assertNotNull(result);
        assertEquals(from, result.from());
        assertEquals(to, result.to());
        assertEquals(NOW, result.generatedAt());
        assertTrue(result.hasTransactions());
        assertEquals(1, result.items().size());

        InventoryInOutStockItemResult item = result.items().get(0);
        assertEquals(medId, item.medicineId());
        assertEquals("MED-001", item.medicineCode());
        assertEquals("Paracetamol 500mg", item.medicineName());
        assertEquals("Viên", item.unit());
        assertEquals(50, item.openingStock());
        assertEquals(100, item.importQuantity());
        assertEquals(30, item.dispensedQuantity());
        assertEquals(10, item.returnedQuantity());
        assertEquals(-5, item.adjustedQuantity());
        // Closing stock = 50 + 100 - 30 + 10 + (-5) = 125
        assertEquals(125, item.closingStock());

        assertEquals(1, result.summary().totalMedicines());
        assertEquals(50, result.summary().totalOpeningStock());
        assertEquals(100, result.summary().totalImportQuantity());
        assertEquals(30, result.summary().totalDispensedQuantity());
        assertEquals(10, result.summary().totalReturnedQuantity());
        assertEquals(-5, result.summary().totalAdjustedQuantity());
        assertEquals(125, result.summary().totalClosingStock());
    }

    @Test
    void successfullyHandlesEmptyPeriod_openingEqualsClosingAndHasTransactionsFalse() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        UUID medId = UUID.randomUUID();
        Medicine medicine = createMedicine(medId, "MED-002", "Amoxicillin 500mg", "Viên");

        when(medicineRepository.findAll()).thenReturn(List.of(medicine));
        when(stockMovementRepository.sumQuantitiesBefore(any(Instant.class)))
                .thenReturn(List.of(new MedicineStockQuantityResult(medId, 40L)));
        when(stockMovementRepository.sumMovementsBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        GetInventoryInOutStockReportQuery query = new GetInventoryInOutStockReportQuery(from, to, null, null);
        InventoryInOutStockReportResult result = service.getReport(query);

        assertNotNull(result);
        assertFalse(result.hasTransactions());
        assertEquals(1, result.items().size());

        InventoryInOutStockItemResult item = result.items().get(0);
        assertEquals(40, item.openingStock());
        assertEquals(0, item.importQuantity());
        assertEquals(0, item.dispensedQuantity());
        assertEquals(0, item.returnedQuantity());
        assertEquals(0, item.adjustedQuantity());
        assertEquals(40, item.closingStock());
    }

    @Test
    void successfullyFiltersByMedicineId() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        UUID medId = UUID.randomUUID();
        Medicine medicine = createMedicine(medId, "MED-001", "Paracetamol 500mg", "Viên");

        when(medicineRepository.findById(medId)).thenReturn(Optional.of(medicine));
        when(stockMovementRepository.sumQuantitiesBeforeForMedicine(eq(medId), any(Instant.class)))
                .thenReturn(List.of(new MedicineStockQuantityResult(medId, 20L)));
        when(stockMovementRepository.sumMovementsBetweenForMedicine(eq(medId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(new MedicineMovementSummaryResult(medId, StockMovementType.RECEIPT, 10L)));

        GetInventoryInOutStockReportQuery query = new GetInventoryInOutStockReportQuery(from, to, medId, null);
        InventoryInOutStockReportResult result = service.getReport(query);

        assertNotNull(result);
        assertEquals(1, result.items().size());
        assertEquals(medId, result.items().get(0).medicineId());
        assertEquals(20, result.items().get(0).openingStock());
        assertEquals(10, result.items().get(0).importQuantity());
        assertEquals(30, result.items().get(0).closingStock());
    }

    @Test
    void throwsWhenMedicineNotFound() {
        UUID medId = UUID.randomUUID();
        when(medicineRepository.findById(medId)).thenReturn(Optional.empty());

        GetInventoryInOutStockReportQuery query = new GetInventoryInOutStockReportQuery(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                medId,
                null
        );

        ValidationException ex = assertThrows(ValidationException.class, () -> service.getReport(query));
        assertTrue(ex.getMessage().contains("Medicine not found"));
    }

    @Test
    void throwsWhenDateRangeInvalid() {
        // from after to
        assertThrows(ValidationException.class, () -> service.getReport(new GetInventoryInOutStockReportQuery(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 8, 1),
                null,
                null
        )));

        // range > 366 days
        assertThrows(ValidationException.class, () -> service.getReport(new GetInventoryInOutStockReportQuery(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 1, 10),
                null,
                null
        )));

        // null dates
        assertThrows(ValidationException.class, () -> service.getReport(new GetInventoryInOutStockReportQuery(
                null,
                LocalDate.of(2026, 8, 1),
                null,
                null
        )));
    }

    @Test
    void throwsWhenAuthorizerDeniesAccess() {
        doThrow(new AccessDeniedException("Forbidden")).when(authorizer).requireReportAccess();

        GetInventoryInOutStockReportQuery query = new GetInventoryInOutStockReportQuery(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                null,
                null
        );

        assertThrows(AccessDeniedException.class, () -> service.getReport(query));
        verify(authorizer).requireReportAccess();
    }

    private static Medicine createMedicine(UUID id, String code, String name, String unit) {
        return Medicine.create(
                id,
                code,
                name,
                "ActiveIngredient",
                "500mg",
                DosageForm.TABLET,
                unit,
                AdministrationRoute.ORAL,
                10,
                NOW
        );
    }
}

