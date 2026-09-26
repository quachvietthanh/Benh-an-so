package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.prescription.PrescriptionAmendment;
import com.benhsoan.persistence.entity.prescription.PrescriptionAmendmentEntity;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionAmendmentRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionAmendmentPersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionAmendmentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PrescriptionAmendmentRepositoryAdapter
        implements PrescriptionAmendmentRepository {

    private final JpaPrescriptionAmendmentRepository jpaRepository;

    private final PrescriptionAmendmentPersistenceMapper mapper;

    @Override
    public PrescriptionAmendment save(PrescriptionAmendment amendment) {
        PrescriptionAmendmentEntity entity = mapper.toEntity(amendment);
        PrescriptionAmendmentEntity savedEntity = jpaRepository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PrescriptionAmendment> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<PrescriptionAmendment> findByPrescriptionId(
            UUID prescriptionId
    ) {
        return jpaRepository
                .findByPrescriptionIdOrderByAmendedAtAsc(prescriptionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
