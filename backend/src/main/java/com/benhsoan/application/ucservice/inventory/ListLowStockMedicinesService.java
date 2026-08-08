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
    private final LowStockEvaluator lowStockEvaluator;

    @Override
    public List<LowStockMedicineResult> list() {
        authorizer.requirePharmacistOrAdmin();

        LocalDate today = LocalDate.ofInstant(clockPort.now(), ZoneOffset.UTC);
        List<Medicine> medicines = medicineRepository.findAllActive();
        Map<java.util.UUID, List<MedicineBatch>> batchesByMedicineId = medicineBatchRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(MedicineBatch::getMedicineId));

        return medicines.stream()
                .map(medicine -> toResult(
                        medicine,
                        batchesByMedicineId.getOrDefault(medicine.getId(), List.of()),
                        today
                ))
                .filter(result -> lowStockEvaluator.isLowStockByThreshold(
                        result.eligibleStockQuantity(),
                        result.minStockThreshold()
                ))
                .toList();
    }

    private LowStockMedicineResult toResult(Medicine medicine, List<MedicineBatch> batches, LocalDate today) {
        int eligibleStockQuantity = lowStockEvaluator.calculateEligibleStockQuantity(batches, today);
        return lowStockEvaluator.toLowStockResult(medicine, eligibleStockQuantity);
    }
}
