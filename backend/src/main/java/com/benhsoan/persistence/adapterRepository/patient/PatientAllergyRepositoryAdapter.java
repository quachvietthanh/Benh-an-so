package com.benhsoan.persistence.adapterRepository.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.domain.patient.exception.PatientAllergyAlreadyExistsException;
import com.benhsoan.persistence.entity.patient.PatientAllergyEntity;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientAllergyRepository;
import com.benhsoan.persistence.mapper.patient.PatientAllergyPersistenceMapper;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PatientAllergyRepositoryAdapter implements PatientAllergyRepository {

    private final JpaPatientAllergyRepository jpaRepository;
    private final PatientAllergyPersistenceMapper mapper;

    @Override
    public PatientAllergy save(PatientAllergy allergy) {
        try {
            PatientAllergyEntity entity = mapper.toEntity(allergy);
            PatientAllergyEntity saved = jpaRepository.saveAndFlush(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new PatientAllergyAlreadyExistsException(allergy.getAllergenName());
        }
    }

    @Override
    public Optional<PatientAllergy> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PatientAllergy> findByPatientIdAndActiveTrue(UUID patientId) {
        return jpaRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByPatientIdAndNormalizedAllergenNameAndActiveTrue(UUID patientId, String normalizedAllergenName) {
        return jpaRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrue(patientId, normalizedAllergenName);
    }

    @Override
    public boolean existsByPatientIdAndNormalizedAllergenNameAndActiveTrueAndIdNot(
            UUID patientId,
            String normalizedAllergenName,
            UUID allergyId
    ) {
        return jpaRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrueAndIdNot(
                patientId,
                normalizedAllergenName,
                allergyId
        );
    }
}
