package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordDiagnosisRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordDiagnosisPersistenceMapper;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicalRecordDiagnosisRepositoryAdapter implements MedicalRecordDiagnosisRepository {

    private final JpaMedicalRecordDiagnosisRepository jpaRepository;
    private final MedicalRecordDiagnosisPersistenceMapper mapper;

    @Override
    public boolean existsByMedicalRecordId(UUID medicalRecordId) {
        return jpaRepository.existsByMedicalRecordId(medicalRecordId);
    }

    @Override
    public boolean existsByDiagnosisCatalogId(UUID diagnosisCatalogId) {
        return jpaRepository.existsByDiagnosisCatalogId(diagnosisCatalogId);
    }

    @Override
    public List<MedicalRecordDiagnosis> findByMedicalRecordId(UUID medicalRecordId) {
        return jpaRepository.findByMedicalRecordIdOrdered(medicalRecordId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MedicalRecordDiagnosis> findByMedicalRecordIdAndDiagnosisType(
            UUID medicalRecordId,
            com.benhsoan.domain.medicalrecord.enums.DiagnosisType diagnosisType
    ) {
        return jpaRepository.findByMedicalRecordIdAndDiagnosisTypeOrdered(medicalRecordId, diagnosisType).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MedicalRecordDiagnosis> findByMedicalRecordIdIn(Collection<UUID> medicalRecordIds) {
        if (medicalRecordIds == null || medicalRecordIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByMedicalRecordIdIn(medicalRecordIds).stream()
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

    @Override
    public List<UUID> findRecentCatalogIdsByDoctor(UUID doctorId, int limit) {
        return jpaRepository.findRecentCatalogIdsByDoctor(doctorId, PageRequest.of(0, limit));
    }

    @Override
    public List<UUID> findPopularCatalogIdsBySpecialty(UUID specialtyId, int limit) {
        return jpaRepository.findPopularCatalogIdsBySpecialty(specialtyId, PageRequest.of(0, limit));
    }
}
