package com.benhsoan.persistence.adapterRepository.medicalrecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAccessLogRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.MedicalRecordAccessLogSpecification;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordAccessLogPersistenceMapper;
import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;

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

    @Override
    public Page<MedicalRecordAccessLog> search(
            GetMedicalRecordAccessLogsQuery query,
            Pageable pageable
    ) {
        return jpaRepository.findAll(MedicalRecordAccessLogSpecification.build(query), pageable)
                .map(mapper::toDomain);
    }
}
