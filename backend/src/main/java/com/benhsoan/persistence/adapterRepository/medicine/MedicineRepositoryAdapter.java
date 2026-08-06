package com.benhsoan.persistence.adapterRepository.medicine;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.persistence.entity.medicine.MedicineEntity;
import com.benhsoan.persistence.jpaRepository.medicine.JpaMedicineRepository;
import com.benhsoan.persistence.mapper.medicine.MedicinePersistenceMapper;
import com.benhsoan.persistence.jpaRepository.medicine.MedicineSpecifications;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineSearchCriteria;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicineRepositoryAdapter
        implements MedicineRepository {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.asc("medicineName"),
            Sort.Order.asc("medicineCode")
    );

    private final JpaMedicineRepository jpaRepository;

    private final MedicinePersistenceMapper mapper;

    @Override
    public Optional<Medicine> findById(UUID id) {
        Objects.requireNonNull(id, "Medicine id must not be null.");

        return jpaRepository.findOne(MedicineSpecifications.hasId(id))
                .map(mapper::toDomain);
    }

    @Override
    public List<Medicine> findAllByIds(Collection<UUID> ids) {
        Objects.requireNonNull(ids, "Medicine ids must not be null.");

        List<UUID> distinctIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) {
            return List.of();
        }

        return mapToDomain(
                jpaRepository.findAll(
                        MedicineSpecifications.hasIdIn(distinctIds),
                        DEFAULT_SORT
                )
        );
    }

    @Override
    public List<Medicine> findAllById(Collection<UUID> ids) {
        return jpaRepository.findAllById(ids)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Medicine> findByMedicineCode(String medicineCode) {
        Objects.requireNonNull(
                medicineCode,
                "Medicine code must not be null."
        );

        return jpaRepository.findOne(
                        MedicineSpecifications.hasMedicineCode(medicineCode)
                )
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByMedicineCode(String medicineCode) {
        Objects.requireNonNull(
                medicineCode,
                "Medicine code must not be null."
        );

        return jpaRepository.exists(
                MedicineSpecifications.hasMedicineCode(medicineCode)
        );
    }

    @Override
    public boolean existsByMedicineNameAndActiveIngredient(
            String medicineName,
            String activeIngredient,
            UUID excludedId
    ) {
        Specification<MedicineEntity> specification =
                MedicineSpecifications.hasMedicineNameIgnoreCase(
                                medicineName
                        )
                        .and(MedicineSpecifications
                                .hasActiveIngredientIgnoreCase(
                                        activeIngredient
                                ))
                        .and(MedicineSpecifications.hasIdNotEqual(
                                excludedId
                        ));

        return jpaRepository.exists(specification);
    }

    @Override
    public Optional<Medicine> findTopByOrderByMedicineCodeDesc() {
        PageRequest firstResult = PageRequest.of(
                0,
                1,
                Sort.by(Sort.Direction.DESC, "medicineCode")
        );

        return jpaRepository.findAll(
                        MedicineSpecifications.all(),
                        firstResult
                )
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public List<Medicine> findAllActive() {
        return mapToDomain(
                jpaRepository.findAll(
                        MedicineSpecifications.hasActive(true),
                        DEFAULT_SORT
                )
        );
    }

    @Override
    public Page<Medicine> search(
            MedicineSearchCriteria criteria,
            Pageable pageable
    ) {
        Objects.requireNonNull(
                criteria,
                "Medicine search criteria must not be null."
        );
        Objects.requireNonNull(
                pageable,
                "Medicine pageable must not be null."
        );

        Specification<MedicineEntity> specification =
                MedicineSpecifications.containsKeyword(criteria.keyword())
                        .and(MedicineSpecifications.hasDosageForm(
                                criteria.dosageForm()
                        ))
                        .and(MedicineSpecifications.hasDefaultRoute(
                                criteria.defaultRoute()
                        ))
                        .and(MedicineSpecifications.hasActive(
                                criteria.active()
                        ));

        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        DEFAULT_SORT
                );

        return jpaRepository.findAll(specification, effectivePageable)
                .map(mapper::toDomain);
    }

    @Override
    public Medicine save(Medicine medicine) {
        Objects.requireNonNull(medicine, "Medicine must not be null.");

        MedicineEntity entity = mapper.toEntity(medicine);
        MedicineEntity savedEntity = jpaRepository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    private List<Medicine> mapToDomain(List<MedicineEntity> entities) {
        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }
}
