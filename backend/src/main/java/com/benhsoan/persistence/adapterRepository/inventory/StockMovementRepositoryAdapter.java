package com.benhsoan.persistence.adapterRepository.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.inventory.StockMovement;
import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.persistence.entity.inventory.StockMovementEntity;
import com.benhsoan.persistence.jpaRepository.inventory.JpaStockMovementRepository;
import com.benhsoan.persistence.mapper.inventory.StockMovementPersistenceMapper;
import com.benhsoan.port.dto.result.inventory.MedicineMovementSummaryResult;
import com.benhsoan.port.dto.result.inventory.MedicineStockQuantityResult;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StockMovementRepositoryAdapter implements StockMovementRepository {

    private final JpaStockMovementRepository jpaRepository;
    private final StockMovementPersistenceMapper mapper;

    @Override
    public StockMovement save(StockMovement stockMovement) {
        StockMovementEntity entity = mapper.toEntity(stockMovement);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<StockMovement> saveAll(List<StockMovement> stockMovements) {
        return jpaRepository.saveAll(
                        stockMovements.stream()
                                .map(mapper::toEntity)
                                .toList()
                ).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<StockMovement> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<StockMovement> findByMedicineBatchId(UUID medicineBatchId) {
        return jpaRepository.findByMedicineBatchIdOrderByPerformedAtAsc(medicineBatchId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<StockMovement> findByReference(StockMovementReferenceType referenceType, UUID referenceId) {
        return jpaRepository.findByReferenceTypeAndReferenceIdOrderByPerformedAtAsc(referenceType, referenceId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MedicineStockQuantityResult> sumQuantitiesBefore(Instant beforeInstant) {
        return jpaRepository.sumQuantitiesBefore(beforeInstant).stream()
                .map(proj -> new MedicineStockQuantityResult(
                        proj.getMedicineId(),
                        proj.getTotalQuantity() != null ? proj.getTotalQuantity() : 0L
                ))
                .toList();
    }

    @Override
    public List<MedicineStockQuantityResult> sumQuantitiesBeforeForMedicine(UUID medicineId, Instant beforeInstant) {
        return jpaRepository.sumQuantitiesBeforeForMedicine(medicineId, beforeInstant).stream()
                .map(proj -> new MedicineStockQuantityResult(
                        proj.getMedicineId(),
                        proj.getTotalQuantity() != null ? proj.getTotalQuantity() : 0L
                ))
                .toList();
    }

    @Override
    public List<MedicineMovementSummaryResult> sumMovementsBetween(Instant from, Instant to) {
        return jpaRepository.sumMovementsBetween(from, to).stream()
                .map(proj -> new MedicineMovementSummaryResult(
                        proj.getMedicineId(),
                        proj.getMovementType(),
                        proj.getTotalQuantityChange() != null ? proj.getTotalQuantityChange() : 0L
                ))
                .toList();
    }

    @Override
    public List<MedicineMovementSummaryResult> sumMovementsBetweenForMedicine(UUID medicineId, Instant from, Instant to) {
        return jpaRepository.sumMovementsBetweenForMedicine(medicineId, from, to).stream()
                .map(proj -> new MedicineMovementSummaryResult(
                        proj.getMedicineId(),
                        proj.getMovementType(),
                        proj.getTotalQuantityChange() != null ? proj.getTotalQuantityChange() : 0L
                ))
                .toList();
    }
}

