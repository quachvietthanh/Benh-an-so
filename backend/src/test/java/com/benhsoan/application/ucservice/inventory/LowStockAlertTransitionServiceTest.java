package com.benhsoan.application.ucservice.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.inventory.InventoryAlertLog;
import com.benhsoan.domain.inventory.enums.InventoryAlertType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.port.outbound.repository.inventory.InventoryAlertLogRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

class LowStockAlertTransitionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T00:00:00Z");

    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final InventoryAlertLogRepository inventoryAlertLogRepository = mock(InventoryAlertLogRepository.class);
    private final EligibleStockSnapshotService eligibleStockSnapshotService = mock(EligibleStockSnapshotService.class);
    private final LowStockEvaluator lowStockEvaluator = new LowStockEvaluator();

    private LowStockAlertTransitionService service;

    @BeforeEach
    void setUp() {
        service = new LowStockAlertTransitionService(
                medicineRepository,
                inventoryAlertLogRepository,
                eligibleStockSnapshotService,
                lowStockEvaluator
        );
    }

    @Test
    void createsAlertWhenMedicineTransitionsIntoLowStock() {
        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine(medicineId, 40)));
        when(inventoryAlertLogRepository.findActiveByMedicineIdAndAlertType(medicineId, InventoryAlertType.LOW_STOCK))
                .thenReturn(Optional.empty());

        service.handleEligibleStockTransitions(
                Map.of(medicineId, 50),
                Map.of(medicineId, 15),
                NOW
        );

        verify(inventoryAlertLogRepository).save(any(InventoryAlertLog.class));
    }

    @Test
    void doesNotCreateDuplicateAlertWhenActiveAlertAlreadyExists() {
        UUID medicineId = UUID.randomUUID();
        when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine(medicineId, 40)));
        when(inventoryAlertLogRepository.findActiveByMedicineIdAndAlertType(medicineId, InventoryAlertType.LOW_STOCK))
                .thenReturn(Optional.of(InventoryAlertLog.createLowStock(medicineId, 40, 15, NOW.minusSeconds(60))));

        service.handleEligibleStockTransitions(
                Map.of(medicineId, 50),
                Map.of(medicineId, 15),
                NOW
        );

        verify(inventoryAlertLogRepository, never()).save(any(InventoryAlertLog.class));
    }

    @Test
    void resolvesAlertWhenMedicineRecoversFromLowStock() {
        UUID medicineId = UUID.randomUUID();
        InventoryAlertLog activeAlert = InventoryAlertLog.createLowStock(medicineId, 40, 15, NOW.minusSeconds(60));
        when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine(medicineId, 40)));
        when(inventoryAlertLogRepository.findActiveByMedicineIdAndAlertType(medicineId, InventoryAlertType.LOW_STOCK))
                .thenReturn(Optional.of(activeAlert));

        service.handleEligibleStockTransitions(
                Map.of(medicineId, 15),
                Map.of(medicineId, 45),
                NOW
        );

        verify(inventoryAlertLogRepository).save(activeAlert);
    }

    @Test
    void doesNotCreateAlertWhenDispenseLeavesEligibleStockAboveThreshold() {
        UUID medicineId = UUID.randomUUID();
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(List.of(medicineId), LocalDate.of(2026, 8, 8)))
                .thenReturn(Map.of(medicineId, 60));
        when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine(medicineId, 40)));

        service.handleEligibleStockTransitions(
                List.of(medicineId),
                Map.of(medicineId, 130),
                LocalDate.of(2026, 8, 8),
                NOW
        );

        verify(inventoryAlertLogRepository, never()).findActiveByMedicineIdAndAlertType(any(), any());
        verify(inventoryAlertLogRepository, never()).save(any(InventoryAlertLog.class));
    }

    @Test
    void doesNotResolveAlertWhenAfterSnapshotStaysLow() {
        UUID medicineId = UUID.randomUUID();
        InventoryAlertLog activeAlert = InventoryAlertLog.createLowStock(medicineId, 40, 15, NOW.minusSeconds(60));
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(List.of(medicineId), LocalDate.of(2026, 8, 8)))
                .thenReturn(Map.of(medicineId, 15));
        when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine(medicineId, 40)));
        when(inventoryAlertLogRepository.findActiveByMedicineIdAndAlertType(medicineId, InventoryAlertType.LOW_STOCK))
                .thenReturn(Optional.of(activeAlert));

        service.handleEligibleStockTransitions(
                List.of(medicineId),
                Map.of(medicineId, 15),
                LocalDate.of(2026, 8, 8),
                NOW
        );

        verify(inventoryAlertLogRepository, never()).save(activeAlert);
    }

    @Test
    void snapshotsAfterEligibleStockThroughSharedService() {
        UUID medicineId = UUID.randomUUID();
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(List.of(medicineId), LocalDate.of(2026, 8, 8)))
                .thenReturn(Map.of(medicineId, 15));
        when(medicineRepository.findAllById(any())).thenReturn(List.of(medicine(medicineId, 40)));
        when(inventoryAlertLogRepository.findActiveByMedicineIdAndAlertType(medicineId, InventoryAlertType.LOW_STOCK))
                .thenReturn(Optional.empty());

        service.handleEligibleStockTransitions(
                List.of(medicineId),
                Map.of(medicineId, 50),
                LocalDate.of(2026, 8, 8),
                NOW
        );

        verify(eligibleStockSnapshotService).snapshotEligibleStockQuantities(List.of(medicineId), LocalDate.of(2026, 8, 8));
        verify(inventoryAlertLogRepository).save(any(InventoryAlertLog.class));
    }

    private Medicine medicine(UUID id, int minStockThreshold) {
        return Medicine.restore(
                id,
                "MED-001",
                "Paracetamol",
                "Paracetamol",
                "500 mg",
                DosageForm.TABLET,
                "viên",
                AdministrationRoute.ORAL,
                true,
                NOW.minusSeconds(3600),
                null,
                100,
                minStockThreshold
        );
    }
}
