package com.benhsoan.persistence.adapterRepository.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.patient.PatientChronicDisease;
import com.benhsoan.domain.patient.exception.PatientChronicDiseaseAlreadyExistsException;
import com.benhsoan.persistence.entity.patient.PatientChronicDiseaseEntity;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientChronicDiseaseRepository;
import com.benhsoan.persistence.mapper.patient.PatientChronicDiseasePersistenceMapper;
import com.benhsoan.port.outbound.repository.patient.PatientChronicDiseaseRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PatientChronicDiseaseRepositoryAdapter implements PatientChronicDiseaseRepository {

    private final JpaPatientChronicDiseaseRepository jpaRepository;
    private final PatientChronicDiseasePersistenceMapper mapper;

    @Override
    public PatientChronicDisease save(PatientChronicDisease chronicDisease) {
        try {
            PatientChronicDiseaseEntity entity = mapper.toEntity(chronicDisease);
            PatientChronicDiseaseEntity saved = jpaRepository.saveAndFlush(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateChronicDiseaseConstraint(e)) {
                throw new PatientChronicDiseaseAlreadyExistsException();
            }
            throw e;
        }
    }

    private boolean isDuplicateChronicDiseaseConstraint(DataIntegrityViolationException ex) {
        String message = extractMessage(ex).toLowerCase();
        return message.contains("uk_patient_active_chronic_disease");
    }

    private String extractMessage(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return builder.toString();
    }

    @Override
    public Optional<PatientChronicDisease> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PatientChronicDisease> findByPatientIdAndActiveTrue(UUID patientId) {
        return jpaRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByPatientIdAndDiagnosisCatalogIdAndActiveTrue(UUID patientId, UUID diagnosisCatalogId) {
        return jpaRepository.existsByPatientIdAndDiagnosisCatalogIdAndActiveTrue(patientId, diagnosisCatalogId);
    }
}
