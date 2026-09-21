package com.benhsoan.persistence.jpaRepository.inventory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.persistence.entity.inventory.StockMovementEntity;

public interface JpaStockMovementRepository
        extends JpaRepository<StockMovementEntity, UUID> {

    List<StockMovementEntity> findByMedicineBatchIdOrderByPerformedAtAsc(UUID medicineBatchId);

    List<StockMovementEntity> findByReferenceTypeAndReferenceIdOrderByPerformedAtAsc(
            StockMovementReferenceType referenceType,
            UUID referenceId
    );

    @Query("select sm.medicineId as medicineId, sum(sm.quantityChange) as totalQuantity "
            + "from StockMovementEntity sm "
            + "where sm.performedAt < :beforeInstant "
            + "group by sm.medicineId")
    List<MedicineStockQuantityProjection> sumQuantitiesBefore(
            @Param("beforeInstant") Instant beforeInstant
    );

    @Query("select sm.medicineId as medicineId, sum(sm.quantityChange) as totalQuantity "
            + "from StockMovementEntity sm "
            + "where sm.medicineId = :medicineId and sm.performedAt < :beforeInstant "
            + "group by sm.medicineId")
    List<MedicineStockQuantityProjection> sumQuantitiesBeforeForMedicine(
            @Param("medicineId") UUID medicineId,
            @Param("beforeInstant") Instant beforeInstant
    );

    @Query("select sm.medicineId as medicineId, sum(sm.quantityChange) as totalQuantity "
            + "from StockMovementEntity sm "
            + "where sm.medicineId in :medicineIds and sm.performedAt < :beforeInstant "
            + "group by sm.medicineId")
    List<MedicineStockQuantityProjection> sumQuantitiesBeforeForMedicineIds(
            @Param("medicineIds") Collection<UUID> medicineIds,
            @Param("beforeInstant") Instant beforeInstant
    );

    @Query("select sm.medicineId as medicineId, sm.movementType as movementType, sum(sm.quantityChange) as totalQuantityChange "
            + "from StockMovementEntity sm "
            + "where sm.performedAt >= :fromInstant and sm.performedAt < :toInstant "
            + "group by sm.medicineId, sm.movementType")
    List<MedicinePeriodMovementProjection> sumMovementsBetween(
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant
    );

    @Query("select sm.medicineId as medicineId, sm.movementType as movementType, sum(sm.quantityChange) as totalQuantityChange "
            + "from StockMovementEntity sm "
            + "where sm.medicineId = :medicineId and sm.performedAt >= :fromInstant and sm.performedAt < :toInstant "
            + "group by sm.medicineId, sm.movementType")
    List<MedicinePeriodMovementProjection> sumMovementsBetweenForMedicine(
            @Param("medicineId") UUID medicineId,
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant
    );

    @Query("select sm.medicineId as medicineId, sm.movementType as movementType, sum(sm.quantityChange) as totalQuantityChange "
            + "from StockMovementEntity sm "
            + "where sm.medicineId in :medicineIds and sm.performedAt >= :fromInstant and sm.performedAt < :toInstant "
            + "group by sm.medicineId, sm.movementType")
    List<MedicinePeriodMovementProjection> sumMovementsBetweenForMedicineIds(
            @Param("medicineIds") Collection<UUID> medicineIds,
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant
    );
}
