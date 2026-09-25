package com.benhsoan.port.outbound.repository.inventory;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;

public interface MedicationProcurementPlanRepository {

    MedicationProcurementPlan save(MedicationProcurementPlan plan);

    Optional<MedicationProcurementPlan> findById(UUID id);

    Optional<MedicationProcurementPlan> findByIdForUpdate(UUID id);

    Optional<MedicationProcurementPlan> findByPlanCode(String planCode);

    Page<MedicationProcurementPlan> findAll(MedicationProcurementPlanSearchCriteria criteria, Pageable pageable);
}
