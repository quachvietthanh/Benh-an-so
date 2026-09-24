package com.benhsoan.persistence.jpaRepository.inventory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.inventory.MedicationProcurementPlanEntity;

import jakarta.persistence.LockModeType;

public interface JpaMedicationProcurementPlanRepository
        extends JpaRepository<MedicationProcurementPlanEntity, UUID>,
                JpaSpecificationExecutor<MedicationProcurementPlanEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM MedicationProcurementPlanEntity p WHERE p.id = :id")
    Optional<MedicationProcurementPlanEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<MedicationProcurementPlanEntity> findByPlanCode(String planCode);

    boolean existsByPlanCode(String planCode);
}
