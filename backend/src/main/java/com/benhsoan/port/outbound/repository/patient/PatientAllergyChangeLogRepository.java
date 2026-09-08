package com.benhsoan.port.outbound.repository.patient;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.patient.PatientAllergyChangeLog;

public interface PatientAllergyChangeLogRepository {

    PatientAllergyChangeLog save(PatientAllergyChangeLog changeLog);

    List<PatientAllergyChangeLog> findByAllergyIdOrderByChangedAtDesc(UUID allergyId);

    List<PatientAllergyChangeLog> findByPatientIdOrderByChangedAtDesc(UUID patientId);
}
