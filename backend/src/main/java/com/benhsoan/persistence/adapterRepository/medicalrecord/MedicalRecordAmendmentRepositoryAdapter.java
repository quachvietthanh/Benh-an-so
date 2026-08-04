package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAmendmentEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAmendmentRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordAmendmentPersistenceMapper;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAmendmentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicalRecordAmendmentRepositoryAdapter implements MedicalRecordAmendmentRepository {

    private final JpaMedicalRecordAmendmentRepository jpaRepository;
    private final MedicalRecordAmendmentPersistenceMapper mapper;

    @Override
    public MedicalRecordAmendment save(MedicalRecordAmendment amendment) {
        MedicalRecordAmendmentEntity savedEntity = jpaRepository.save(mapper.toEntity(amendment));
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<MedicalRecordAmendment> findByMedicalRecordId(UUID medicalRecordId) {
        return jpaRepository.findByMedicalRecordIdOrderByAmendedAtDesc(medicalRecordId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
