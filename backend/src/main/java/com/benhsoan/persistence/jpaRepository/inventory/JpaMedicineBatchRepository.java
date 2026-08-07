package com.benhsoan.persistence.jpaRepository.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.inventory.MedicineBatchEntity;

import jakarta.persistence.LockModeType;

public interface JpaMedicineBatchRepository
        extends JpaRepository<MedicineBatchEntity, UUID> {

    Optional<MedicineBatchEntity> findByMedicineIdAndBatchNumber(
            UUID medicineId, String batchNumber);

    @Query("""
            select batch
            from MedicineBatchEntity batch
            where batch.medicineId = :medicineId
              and batch.status = com.benhsoan.domain.inventory.enums.BatchStatus.ACTIVE
              and batch.quantity > 0
              and batch.expiryDate >= :today
            order by batch.expiryDate asc, batch.createdAt asc
            """)
    List<MedicineBatchEntity> findAvailableByMedicineId(
            @Param("medicineId") UUID medicineId,
            @Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select batch
            from MedicineBatchEntity batch
            where batch.medicineId = :medicineId
              and batch.status = com.benhsoan.domain.inventory.enums.BatchStatus.ACTIVE
              and batch.quantity > 0
              and batch.expiryDate >= :today
            order by batch.expiryDate asc, batch.createdAt asc
            """)
    List<MedicineBatchEntity> findAvailableByMedicineIdForUpdate(
            @Param("medicineId") UUID medicineId,
            @Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE MedicineBatchEntity b SET b.quantity = b.quantity + :delta WHERE b.id = :id")
    int addStockQuantity(@Param("id") UUID id, @Param("delta") int delta);

    @Modifying
    @Query("""
            UPDATE MedicineBatchEntity b
            SET b.quantity = b.quantity - :delta,
                b.status = :status,
                b.updatedAt = :updatedAt
            WHERE b.id = :id
              AND b.quantity >= :delta
            """)
    int deductStockQuantity(
            @Param("id") UUID id,
            @Param("delta") int delta,
            @Param("status") com.benhsoan.domain.inventory.enums.BatchStatus status,
            @Param("updatedAt") Instant updatedAt);
}
