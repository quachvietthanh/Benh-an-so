package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.prescription.PrescriptionWarningLog;
import com.benhsoan.persistence.entity.prescription.PrescriptionWarningLogEntity;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionWarningLogRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionWarningLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PrescriptionWarningLogRepositoryAdapter
        implements PrescriptionWarningLogRepository {

    private final JpaPrescriptionWarningLogRepository jpaRepository;

    private final PrescriptionWarningLogPersistenceMapper mapper;

    @Override
    public PrescriptionWarningLog save(PrescriptionWarningLog warningLog) {
        PrescriptionWarningLogEntity entity = mapper.toEntity(warningLog);
        PrescriptionWarningLogEntity savedEntity = jpaRepository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PrescriptionWarningLog> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<PrescriptionWarningLog> findByPrescriptionId(
            UUID prescriptionId
    ) {
        return jpaRepository
                .findByPrescriptionIdOrderByCreatedAtAsc(prescriptionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
