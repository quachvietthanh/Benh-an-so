package com.benhsoan.port.outbound.repository.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.patient.PatientConsentRecord;

public interface PatientConsentHistoryRepository {

    PatientConsentRecord save(PatientConsentRecord record);

    List<PatientConsentRecord> findByPatientId(UUID patientId);

    Optional<PatientConsentRecord> findLatestByPatientId(UUID patientId);

    int getNextVersionNumber(UUID patientId);
}
