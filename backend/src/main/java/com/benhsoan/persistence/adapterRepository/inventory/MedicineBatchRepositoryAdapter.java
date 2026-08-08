package com.benhsoan.persistence.adapterRepository.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.persistence.entity.inventory.MedicineBatchEntity;
import com.benhsoan.persistence.jpaRepository.inventory.JpaMedicineBatchRepository;
import com.benhsoan.persistence.mapper.inventory.MedicineBatchPersistenceMapper;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicineBatchRepositoryAdapter implements MedicineBatchRepository {

    private final JpaMedicineBatchRepository jpaRepository;
    private final MedicineBatchPersistenceMapper mapper;

    @Override
    public List<MedicineBatch> findAll() {
        return jpaRepository.findAllByOrderByExpiryDateAscCreatedAtAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MedicineBatch> findByMedicineId(UUID medicineId) {
        Objects.requireNonNull(medicineId, "Medicine id must not be null.");

        return jpaRepository.findByMedicineIdOrderByExpiryDateAscCreatedAtAsc(medicineId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<MedicineBatch> findByMedicineIdAndBatchNumber(
            UUID medicineId, String batchNumber) {
        Objects.requireNonNull(medicineId, "Medicine id must not be null.");
        Objects.requireNonNull(batchNumber, "Batch number must not be null.");

        return jpaRepository.findByMedicineIdAndBatchNumber(medicineId, batchNumber)
                .map(mapper::toDomain);
    }

    @Override
    public List<MedicineBatch> findAvailableByMedicineId(UUID medicineId, LocalDate today) {
        Objects.requireNonNull(medicineId, "Medicine id must not be null.");
        Objects.requireNonNull(today, "Today must not be null.");

        return jpaRepository.findAvailableByMedicineId(medicineId, today)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MedicineBatch> findAvailableByMedicineIdForUpdate(UUID medicineId, LocalDate today) {
        Objects.requireNonNull(medicineId, "Medicine id must not be null.");
        Objects.requireNonNull(today, "Today must not be null.");

        return jpaRepository.findAvailableByMedicineIdForUpdate(medicineId, today)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void addStockQuantity(UUID batchId, int delta) {
        Objects.requireNonNull(batchId, "Batch id must not be null.");
        int updated = jpaRepository.addStockQuantity(batchId, delta);
        if (updated == 0) {
            throw new ValidationException(
                    "Medicine batch not found with id: " + batchId);
        }
    }

    @Override
    public void deductStockQuantity(UUID batchId, int delta, BatchStatus status, Instant updatedAt) {
        Objects.requireNonNull(batchId, "Batch id must not be null.");
        Objects.requireNonNull(status, "Batch status must not be null.");
        Objects.requireNonNull(updatedAt, "Updated at must not be null.");
        if (delta <= 0) {
            throw new ValidationException("Deduction delta must be greater than 0.");
        }

        int updated = jpaRepository.deductStockQuantity(batchId, delta, status, updatedAt);
        if (updated == 0) {
            throw new ValidationException(
                    "Unable to deduct stock for batch id: " + batchId + ". Stock may be insufficient.");
        }
    }

    @Override
    public MedicineBatch save(MedicineBatch batch) {
        Objects.requireNonNull(batch, "Medicine batch must not be null.");

        MedicineBatchEntity entity = mapper.toEntity(batch);
        MedicineBatchEntity savedEntity = jpaRepository.save(entity);

        return mapper.toDomain(savedEntity);
    }
}
