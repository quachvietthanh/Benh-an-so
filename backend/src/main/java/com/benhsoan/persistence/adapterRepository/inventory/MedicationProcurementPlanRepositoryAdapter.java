package com.benhsoan.persistence.adapterRepository.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.procurement.MedicationProcurementItem;
import com.benhsoan.domain.inventory.procurement.MedicationProcurementPlan;
import com.benhsoan.persistence.entity.inventory.MedicationProcurementItemEntity;
import com.benhsoan.persistence.entity.inventory.MedicationProcurementPlanEntity;
import com.benhsoan.persistence.jpaRepository.inventory.JpaMedicationProcurementItemRepository;
import com.benhsoan.persistence.jpaRepository.inventory.JpaMedicationProcurementPlanRepository;
import com.benhsoan.persistence.mapper.inventory.MedicationProcurementPersistenceMapper;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementPlanSearchCriteria;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicationProcurementPlanRepositoryAdapter implements MedicationProcurementPlanRepository {

    private final JpaMedicationProcurementPlanRepository jpaPlanRepository;
    private final JpaMedicationProcurementItemRepository jpaItemRepository;
    private final MedicationProcurementPersistenceMapper mapper;

    @Override
    @Transactional
    public MedicationProcurementPlan save(MedicationProcurementPlan plan) {
        MedicationProcurementPlanEntity planEntity = mapper.toEntity(plan);
        MedicationProcurementPlanEntity savedPlan = jpaPlanRepository.save(planEntity);

        jpaItemRepository.deleteByPlanId(plan.getId());
        jpaItemRepository.flush();
        List<MedicationProcurementItemEntity> itemEntities = new ArrayList<>();
        if (plan.getItems() != null) {
            for (MedicationProcurementItem item : plan.getItems()) {
                itemEntities.add(mapper.toItemEntity(item));
            }
        }
        List<MedicationProcurementItemEntity> savedItems = jpaItemRepository.saveAll(itemEntities);

        return mapper.toDomain(savedPlan, savedItems);
    }

    @Override
    public Optional<MedicationProcurementPlan> findById(UUID id) {
        return jpaPlanRepository.findById(id)
                .map(planEntity -> {
                    List<MedicationProcurementItemEntity> items =
                            jpaItemRepository.findByPlanIdOrderByCreatedAtAsc(planEntity.getId());
                    return mapper.toDomain(planEntity, items);
                });
    }

    @Override
    public Optional<MedicationProcurementPlan> findByIdForUpdate(UUID id) {
        return jpaPlanRepository.findByIdForUpdate(id)
                .map(planEntity -> {
                    List<MedicationProcurementItemEntity> items =
                            jpaItemRepository.findByPlanIdOrderByCreatedAtAsc(planEntity.getId());
                    return mapper.toDomain(planEntity, items);
                });
    }

    @Override
    public Optional<MedicationProcurementPlan> findByPlanCode(String planCode) {
        return jpaPlanRepository.findByPlanCode(planCode)
                .map(planEntity -> {
                    List<MedicationProcurementItemEntity> items =
                            jpaItemRepository.findByPlanIdOrderByCreatedAtAsc(planEntity.getId());
                    return mapper.toDomain(planEntity, items);
                });
    }

    @Override
    public Page<MedicationProcurementPlan> findAll(MedicationProcurementPlanSearchCriteria criteria, Pageable pageable) {
        Specification<MedicationProcurementPlanEntity> spec = buildSpecification(criteria);
        Page<MedicationProcurementPlanEntity> page = jpaPlanRepository.findAll(spec, pageable);

        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        List<UUID> planIds = page.getContent().stream()
                .map(MedicationProcurementPlanEntity::getId)
                .toList();

        Map<UUID, List<MedicationProcurementItemEntity>> itemsByPlanId =
                jpaItemRepository.findByPlanIdInOrderByCreatedAtAsc(planIds).stream()
                        .collect(Collectors.groupingBy(MedicationProcurementItemEntity::getPlanId));

        List<MedicationProcurementPlan> plans = page.getContent().stream()
                .map(planEntity -> {
                    List<MedicationProcurementItemEntity> items =
                            itemsByPlanId.getOrDefault(planEntity.getId(), List.of());
                    return mapper.toDomain(planEntity, items);
                })
                .toList();

        return new PageImpl<>(plans, pageable, page.getTotalElements());
    }

    private Specification<MedicationProcurementPlanEntity> buildSpecification(MedicationProcurementPlanSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                if (criteria.status() != null) {
                    predicates.add(cb.equal(root.get("status"), criteria.status()));
                }
                if (criteria.createdBy() != null) {
                    predicates.add(cb.equal(root.get("createdBy"), criteria.createdBy()));
                }
                if (criteria.fromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("periodStartDate"), criteria.fromDate()));
                }
                if (criteria.toDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("periodEndDate"), criteria.toDate()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
