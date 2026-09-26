package com.benhsoan.persistence.adapterRepository.prescription;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.prescription.PrescriptionMaxDailyDoseWarningLog;
import com.benhsoan.persistence.jpaRepository.prescription.JpaPrescriptionMaxDailyDoseWarningLogRepository;
import com.benhsoan.persistence.mapper.prescription.PrescriptionMaxDailyDoseWarningLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionMaxDailyDoseWarningLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PrescriptionMaxDailyDoseWarningLogRepositoryAdapter
        implements PrescriptionMaxDailyDoseWarningLogRepository {

    private final JpaPrescriptionMaxDailyDoseWarningLogRepository jpaRepository;
    private final PrescriptionMaxDailyDoseWarningLogPersistenceMapper mapper;

    @Override
    public PrescriptionMaxDailyDoseWarningLog save(PrescriptionMaxDailyDoseWarningLog log) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(log)));
    }
}
