package com.benhsoan.application.ucservice.inventory;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.port.dto.result.LowStockMedicineResult;
import com.benhsoan.port.inbound.inventory.ListLowStockMedicinesUseCase;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListLowStockMedicinesService implements ListLowStockMedicinesUseCase {

    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final InventoryManagementAuthorizer authorizer;
    private final ClockPort clockPort;

    @Override
    public List<LowStockMedicineResult> list() {
        authorizer.requirePharmacistOrAdmin();

        LocalDate today = LocalDate.ofInstant(clockPort.now(), ZoneOffset.UTC);
        List<Medicine> medicines = medicineRepository.findAllActive();
        Map<java.util.UUID, List<MedicineBatch>> batchesByMedicineId = medicineBatchRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(MedicineBatch::getMedicineId));

        return medicines.stream()
                .map(medicine -> toResult(medicine, batchesByMedicineId.getOrDefault(medicine.getId(), List.of()), today))
                .filter(result -> result.eligibleStockQuantity() < result.minStockThreshold())
                .toList();
    }

    private LowStockMedicineResult toResult(Medicine medicine, List<MedicineBatch> batches, LocalDate today) {
        int eligibleStockQuantity = batches.stream()
                .filter(batch -> batch.isEligibleForDispenseOn(today))
                .mapToInt(MedicineBatch::getQuantity)
                .sum();
        int minStockThreshold = medicine.getMinStockThreshold();

        return new LowStockMedicineResult(
                medicine.getId(),
                medicine.getMedicineCode(),
                medicine.getMedicineName(),
                medicine.getUnit(),
                medicine.getStockQuantity(),
                eligibleStockQuantity,
                minStockThreshold,
                Math.max(0, minStockThreshold - eligibleStockQuantity)
        );
    }
}
