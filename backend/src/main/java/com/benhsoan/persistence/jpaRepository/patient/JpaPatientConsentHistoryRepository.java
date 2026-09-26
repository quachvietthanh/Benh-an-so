package com.benhsoan.persistence.jpaRepository.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.patient.PatientConsentHistoryEntity;

public interface JpaPatientConsentHistoryRepository
        extends JpaRepository<PatientConsentHistoryEntity, UUID> {

    List<PatientConsentHistoryEntity> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<PatientConsentHistoryEntity> findByPatientIdOrderByVersionNumberDesc(UUID patientId);

    Optional<PatientConsentHistoryEntity> findTopByPatientIdOrderByVersionNumberDesc(UUID patientId);
}
