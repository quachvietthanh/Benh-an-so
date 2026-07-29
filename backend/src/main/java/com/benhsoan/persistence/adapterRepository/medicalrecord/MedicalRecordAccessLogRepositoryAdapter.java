package com.benhsoan.persistence.adapterRepository.medicalrecord;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAccessLogRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordAccessLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.logRepository.MedicalRecordAccessLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicalRecordAccessLogRepositoryAdapter implements MedicalRecordAccessLogRepository {

    private final JpaMedicalRecordAccessLogRepository jpaRepository;
    private final MedicalRecordAccessLogPersistenceMapper mapper;

    @Override
    public MedicalRecordAccessLog save(MedicalRecordAccessLog accessLog) {
        MedicalRecordAccessLogEntity savedEntity = jpaRepository.save(mapper.toEntity(accessLog));
        return mapper.toDomain(savedEntity);
    }
}
