package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.prescription.PrescriptionAllergyWarningLog;
import com.benhsoan.persistence.entity.prescription.PrescriptionAllergyWarningLogEntity;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionAllergyWarningLogRepository;
import com.benhsoan.persistence.jpaRepository.prescription.PrescriptionAllergyWarningLogSpecification;
import com.benhsoan.persistence.mapper.prescription.PrescriptionAllergyWarningLogPersistenceMapper;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionAllergyWarningLogsQuery;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionAllergyWarningLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PrescriptionAllergyWarningLogRepositoryAdapter
        implements PrescriptionAllergyWarningLogRepository {

    private final JpaPrescriptionAllergyWarningLogRepository jpaRepository;
    private final PrescriptionAllergyWarningLogPersistenceMapper mapper;

    @Override
    public PrescriptionAllergyWarningLog save(PrescriptionAllergyWarningLog warningLog) {
        PrescriptionAllergyWarningLogEntity entity = mapper.toEntity(warningLog);
        PrescriptionAllergyWarningLogEntity savedEntity = jpaRepository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<PrescriptionAllergyWarningLog> findByPrescriptionId(UUID prescriptionId) {
        return jpaRepository
                .findByPrescriptionIdOrderByCreatedAtAsc(prescriptionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<PrescriptionAllergyWarningLog> search(
            SearchPrescriptionAllergyWarningLogsQuery query,
            Pageable pageable
    ) {
        Specification<PrescriptionAllergyWarningLogEntity> spec =
                PrescriptionAllergyWarningLogSpecification.build(query);
        return jpaRepository.findAll(spec, pageable)
                .map(mapper::toDomain);
    }
}
