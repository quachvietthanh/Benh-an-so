package com.benhsoan.application.ucservice.inventory;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.port.dto.query.inventory.ListInventoryStocksQuery;
import com.benhsoan.port.dto.result.InventoryStockResult;
import com.benhsoan.port.inbound.inventory.ListInventoryStocksUseCase;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListInventoryStocksService implements ListInventoryStocksUseCase {

    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final InventoryManagementAuthorizer authorizer;
    private final ClockPort clockPort;

    @Override
    public List<InventoryStockResult> list(ListInventoryStocksQuery query) {
        authorizer.requirePharmacistOrAdmin();

        ListInventoryStocksQuery effectiveQuery = query == null
                ? new ListInventoryStocksQuery(null)
                : query;
        LocalDate today = LocalDate.ofInstant(clockPort.now(), ZoneOffset.UTC);

        List<Medicine> medicines = Boolean.TRUE.equals(effectiveQuery.active())
                ? medicineRepository.findAllActive()
                : medicineRepository.findAll();

        Map<java.util.UUID, List<MedicineBatch>> batchesByMedicineId = medicineBatchRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(MedicineBatch::getMedicineId));

        return medicines.stream()
                .map(medicine -> toResult(medicine, batchesByMedicineId.getOrDefault(medicine.getId(), List.of()), today))
                .toList();
    }

    private InventoryStockResult toResult(Medicine medicine, List<MedicineBatch> batches, LocalDate today) {
        int eligibleStockQuantity = batches.stream()
                .filter(batch -> batch.isEligibleForDispenseOn(today))
                .mapToInt(MedicineBatch::getQuantity)
                .sum();
        int activeBatchCount = (int) batches.stream()
                .filter(batch -> batch.getQuantity() > 0)
                .count();
        LocalDate nearestExpiryDate = batches.stream()
                .filter(batch -> batch.getQuantity() > 0)
                .map(MedicineBatch::getExpiryDate)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return new InventoryStockResult(
                medicine.getId(),
                medicine.getMedicineCode(),
                medicine.getMedicineName(),
                medicine.getActiveIngredient(),
                medicine.getStrength(),
                medicine.getUnit(),
                medicine.isActive(),
                medicine.getStockQuantity(),
                eligibleStockQuantity,
                activeBatchCount,
                nearestExpiryDate
        );
    }
}
