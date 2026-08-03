package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordDiagnosisRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordDiagnosisPersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordDiagnosisRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicalRecordDiagnosisRepositoryAdapter implements MedicalRecordDiagnosisRepository {

    private final JpaMedicalRecordDiagnosisRepository jpaRepository;
    private final MedicalRecordDiagnosisPersistenceMapper mapper;

    @Override
    public List<MedicalRecordDiagnosis> findByMedicalRecordId(UUID medicalRecordId) {
        return jpaRepository.findByMedicalRecordId(medicalRecordId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MedicalRecordDiagnosis> replaceForMedicalRecord(
            UUID medicalRecordId,
            List<MedicalRecordDiagnosis> diagnoses
    ) {
        jpaRepository.deleteByMedicalRecordId(medicalRecordId);
        return jpaRepository.saveAll(diagnoses.stream().map(mapper::toEntity).toList()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
