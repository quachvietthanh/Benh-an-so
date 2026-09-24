package com.benhsoan.persistence.adapterRepository.controlledmedicine;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.controlledmedicine.ControlledMedicineRegister;
import com.benhsoan.persistence.entity.controlledmedicine.ControlledMedicineRegisterEntity;
import com.benhsoan.persistence.jpaRepository.controlledmedicine.ControlledMedicineRegisterSpecifications;
import com.benhsoan.persistence.jpaRepository.controlledmedicine.JpaControlledMedicineRegisterRepository;
import com.benhsoan.persistence.mapper.controlledmedicine.ControlledMedicineRegisterPersistenceMapper;
import com.benhsoan.port.outbound.repository.controlledmedicine.ControlledMedicineRegisterRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ControlledMedicineRegisterRepositoryAdapter
        implements ControlledMedicineRegisterRepository {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("dispensedAt"),
            Sort.Order.desc("createdAt")
    );

    private final JpaControlledMedicineRegisterRepository jpaRepository;

    private final ControlledMedicineRegisterPersistenceMapper mapper;

    @Override
    public ControlledMedicineRegister save(ControlledMedicineRegister record) {
        Objects.requireNonNull(record, "Controlled medicine register record must not be null.");
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(record)));
    }

    @Override
    public List<ControlledMedicineRegister> saveAll(List<ControlledMedicineRegister> records) {
        Objects.requireNonNull(records, "Controlled medicine register records must not be null.");
        if (records.isEmpty()) {
            return List.of();
        }
        return jpaRepository.saveAll(records.stream().map(mapper::toEntity).toList())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<ControlledMedicineRegister> search(
            ControlledMedicineRegisterSearchCriteria criteria,
            Pageable pageable
    ) {
        Objects.requireNonNull(criteria, "Search criteria must not be null.");
        Objects.requireNonNull(pageable, "Pageable must not be null.");

        Specification<ControlledMedicineRegisterEntity> specification =
                ControlledMedicineRegisterSpecifications.hasPatientId(criteria.patientId())
                        .and(ControlledMedicineRegisterSpecifications.hasMedicineId(criteria.medicineId()))
                        .and(ControlledMedicineRegisterSpecifications.dispensedAtBetween(
                                criteria.fromInclusive(),
                                criteria.toExclusive()
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
}
