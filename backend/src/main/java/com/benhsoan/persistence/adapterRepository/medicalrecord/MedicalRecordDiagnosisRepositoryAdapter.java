package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordDiagnosisEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordDiagnosisRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicalRecordDiagnosisRepositoryAdapter implements MedicalRecordDiagnosisRepository {

    private final JpaMedicalRecordDiagnosisRepository jpaRepository;

    @Override
    public List<MedicalRecordDiagnosis> findByMedicalRecordId(UUID medicalRecordId) {
        return jpaRepository.findByMedicalRecordId(medicalRecordId).stream()
                .map(this::toDomain)
                .toList();
    }

    private MedicalRecordDiagnosis toDomain(MedicalRecordDiagnosisEntity entity) {
        return MedicalRecordDiagnosis.restore(
                entity.getId(),
                entity.getMedicalRecordId(),
                entity.getDiagnosisCatalogId(),
                entity.getDiagnosisCode(),
                entity.getDiagnosisName(),
                entity.getDiagnosisType(),
                entity.getNote(),
                entity.getDiagnosedBy(),
                entity.getDiagnosedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
