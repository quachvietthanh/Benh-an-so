package com.benhsoan.persistence.jpaRepository.contraindication;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.contraindication.ContraindicationRuleEntity;

public interface JpaContraindicationRuleRepository
        extends JpaRepository<ContraindicationRuleEntity, UUID> {

    @Query("""
            select rule
            from ContraindicationRuleEntity rule
            where rule.active = true
              and (rule.medicineId in :medicineIds or rule.activeIngredient in :ingredients)
            order by rule.severity desc
            """)
    List<ContraindicationRuleEntity> findActiveByMedicineIdsAndIngredients(
            @Param("medicineIds") Collection<UUID> medicineIds,
            @Param("ingredients") Collection<String> ingredients
    );
}
