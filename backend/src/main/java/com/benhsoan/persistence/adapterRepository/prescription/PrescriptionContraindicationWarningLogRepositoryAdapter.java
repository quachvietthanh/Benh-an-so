package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.prescription.PrescriptionContraindicationWarningLog;
import com.benhsoan.persistence.entity.prescription.PrescriptionContraindicationWarningLogEntity;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionContraindicationWarningLogRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionContraindicationWarningLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionContraindicationWarningLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PrescriptionContraindicationWarningLogRepositoryAdapter
        implements PrescriptionContraindicationWarningLogRepository {

    private final JpaPrescriptionContraindicationWarningLogRepository jpaRepository;
    private final PrescriptionContraindicationWarningLogPersistenceMapper mapper;

    @Override
    public PrescriptionContraindicationWarningLog save(PrescriptionContraindicationWarningLog log) {
        PrescriptionContraindicationWarningLogEntity entity = mapper.toEntity(log);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<PrescriptionContraindicationWarningLog> findByPrescriptionId(UUID prescriptionId) {
        return jpaRepository.findByPrescriptionIdOrderByHandledAtAsc(prescriptionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
