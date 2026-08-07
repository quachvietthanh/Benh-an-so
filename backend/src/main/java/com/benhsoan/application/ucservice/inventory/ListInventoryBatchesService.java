package com.benhsoan.application.ucservice.inventory;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.port.dto.query.inventory.ListInventoryBatchesQuery;
import com.benhsoan.port.dto.result.InventoryBatchResult;
import com.benhsoan.port.inbound.inventory.ListInventoryBatchesUseCase;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListInventoryBatchesService implements ListInventoryBatchesUseCase {

    private final MedicineBatchRepository medicineBatchRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryManagementAuthorizer authorizer;
    private final ClockPort clockPort;

    @Override
    public List<InventoryBatchResult> list(ListInventoryBatchesQuery query) {
        authorizer.requirePharmacistOrAdmin();

        ListInventoryBatchesQuery effectiveQuery = query == null
                ? new ListInventoryBatchesQuery(null, null, null)
                : query;
        LocalDate today = LocalDate.ofInstant(clockPort.now(), ZoneOffset.UTC);

        List<MedicineBatch> batches = effectiveQuery.medicineId() == null
                ? medicineBatchRepository.findAll()
                : medicineBatchRepository.findByMedicineId(effectiveQuery.medicineId());

        Map<UUID, Medicine> medicinesById = medicineRepository.findAllById(
                        batches.stream()
                                .map(MedicineBatch::getMedicineId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        return batches.stream()
                .filter(batch -> effectiveQuery.status() == null || batch.getStatus() == effectiveQuery.status())
                .filter(batch -> effectiveQuery.eligibleForDispense() == null
                        || batch.isEligibleForDispenseOn(today) == effectiveQuery.eligibleForDispense())
                .map(batch -> toResult(batch, medicinesById.get(batch.getMedicineId()), today))
                .toList();
    }

    private InventoryBatchResult toResult(MedicineBatch batch, Medicine medicine, LocalDate today) {
        return new InventoryBatchResult(
                batch.getId(),
                batch.getMedicineId(),
                medicine == null ? null : medicine.getMedicineCode(),
                medicine == null ? null : medicine.getMedicineName(),
                batch.getBatchNumber(),
                batch.getExpiryDate(),
                batch.getQuantity(),
                batch.getStatus(),
                batch.isEligibleForDispenseOn(today),
                batch.getCreatedAt(),
                batch.getUpdatedAt()
        );
    }
}
