package com.benhsoan.port.outbound.repository.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.patient.PatientChronicDisease;

public interface PatientChronicDiseaseRepository {

    PatientChronicDisease save(PatientChronicDisease chronicDisease);

    Optional<PatientChronicDisease> findById(UUID id);

    List<PatientChronicDisease> findByPatientIdAndActiveTrue(UUID patientId);

    boolean existsByPatientIdAndDiagnosisCatalogIdAndActiveTrue(UUID patientId, UUID diagnosisCatalogId);
}
