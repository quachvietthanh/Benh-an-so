package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAccessLogRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.MedicalRecordAccessLogSpecification;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordAccessLogPersistenceMapper;
import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;
import com.benhsoan.port.dto.result.AccessLogAccountCountResult;
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

    @Override
    public List<MedicalRecordAccessLog> findViewsBetween(Instant from, Instant to) {
        return jpaRepository.findByActionAndAccessedAtBetween(MedicalRecordAccessAction.VIEW, from, to)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AccessLogAccountCountResult> countAccessByAccountBetween(Instant from, Instant to) {
        return jpaRepository.countAccessByAccount(MedicalRecordAccessAction.ACCESS_ACTIONS, from, to)
                .stream()
                .map(p -> new AccessLogAccountCountResult(p.getAccessedBy(), p.getAccessCount()))
                .toList();
    }
}
