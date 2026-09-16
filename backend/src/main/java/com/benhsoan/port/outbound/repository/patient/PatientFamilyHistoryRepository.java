package com.benhsoan.port.outbound.repository.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.patient.PatientFamilyHistory;

public interface PatientFamilyHistoryRepository {

    PatientFamilyHistory save(PatientFamilyHistory familyHistory);

    Optional<PatientFamilyHistory> findById(UUID id);

    List<PatientFamilyHistory> findByPatientIdAndActiveTrue(UUID patientId);
}
