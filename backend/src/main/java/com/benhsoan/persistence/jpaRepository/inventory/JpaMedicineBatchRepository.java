package com.benhsoan.persistence.jpaRepository.inventory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.inventory.MedicineBatchEntity;

public interface JpaMedicineBatchRepository
        extends JpaRepository<MedicineBatchEntity, UUID> {

    Optional<MedicineBatchEntity> findByMedicineIdAndBatchNumber(
            UUID medicineId, String batchNumber);

    @Modifying
    @Query("UPDATE MedicineBatchEntity b SET b.quantity = b.quantity + :delta WHERE b.id = :id")
    int addStockQuantity(@Param("id") UUID id, @Param("delta") int delta);
}
