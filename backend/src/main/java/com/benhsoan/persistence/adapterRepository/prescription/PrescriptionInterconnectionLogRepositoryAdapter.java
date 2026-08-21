package com.benhsoan.persistence.adapterRepository.prescription;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.prescription.PrescriptionInterconnectionLog;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionInterconnectionLogRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionInterconnectionLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionInterconnectionLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PrescriptionInterconnectionLogRepositoryAdapter
        implements PrescriptionInterconnectionLogRepository {

    private final JpaPrescriptionInterconnectionLogRepository jpaRepository;
    private final PrescriptionInterconnectionLogPersistenceMapper mapper;

    @Override
    @Transactional
    public PrescriptionInterconnectionLog save(PrescriptionInterconnectionLog log) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(log)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionInterconnectionLog> findByPrescriptionId(UUID prescriptionId) {
        return jpaRepository.findByPrescriptionIdOrderByAttemptNumberAsc(prescriptionId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
