package com.benhsoan.persistence.jpaRepository.contraindication;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;
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

    @Query("""
            select rule
            from ContraindicationRuleEntity rule
            where (:activeIngredient is null or lower(rule.activeIngredient) like lower(concat('%', :activeIngredient, '%')))
              and (:type is null or rule.type = :type)
              and (:severity is null or rule.severity = :severity)
              and (:active is null or rule.active = :active)
            order by rule.createdAt desc
            """)
    Page<ContraindicationRuleEntity> search(
            @Param("activeIngredient") String activeIngredient,
            @Param("type") ContraindicationType type,
            @Param("severity") ContraindicationSeverity severity,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
