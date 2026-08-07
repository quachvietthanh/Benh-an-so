package com.benhsoan.persistence.adapterRepository.inventory;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.inventory.MedicineBatch;
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
    public Optional<MedicineBatch> findByMedicineIdAndBatchNumber(
            UUID medicineId, String batchNumber) {
        Objects.requireNonNull(medicineId, "Medicine id must not be null.");
        Objects.requireNonNull(batchNumber, "Batch number must not be null.");

        return jpaRepository.findByMedicineIdAndBatchNumber(medicineId, batchNumber)
                .map(mapper::toDomain);
    }

    @Override
    public void addStockQuantity(UUID batchId, int delta) {
        Objects.requireNonNull(batchId, "Batch id must not be null.");
        int updated = jpaRepository.addStockQuantity(batchId, delta);
        if (updated == 0) {
            throw new com.benhsoan.domain.shared.exception.ValidationException(
                    "Medicine batch not found with id: " + batchId);
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
