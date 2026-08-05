package com.benhsoan.persistence.jpaRepository.druginteraction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.druginteraction.DrugInteractionEntity;

public interface JpaDrugInteractionRepository
        extends JpaRepository<DrugInteractionEntity, UUID> {

    Optional<DrugInteractionEntity> findByFirstMedicineIdAndSecondMedicineId(
            UUID firstMedicineId,
            UUID secondMedicineId
    );

    Optional<DrugInteractionEntity> findByFirstMedicineIdAndSecondMedicineIdAndActiveTrue(
            UUID firstMedicineId,
            UUID secondMedicineId
    );

    boolean existsByFirstMedicineIdAndSecondMedicineId(
            UUID firstMedicineId,
            UUID secondMedicineId
    );

    @Query("select interaction from DrugInteractionEntity interaction "
            + "where interaction.active = true "
            + "and interaction.firstMedicineId in :medicineIds "
            + "and interaction.secondMedicineId in :medicineIds")
    List<DrugInteractionEntity> findActiveInteractionsAmong(
            @Param("medicineIds") Collection<UUID> medicineIds
    );
}
