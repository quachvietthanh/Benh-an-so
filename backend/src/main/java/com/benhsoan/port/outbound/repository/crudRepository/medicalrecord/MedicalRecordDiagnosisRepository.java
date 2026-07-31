package com.benhsoan.port.outbound.repository.crudRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;

/**
 * Outbound query repository for medical record diagnoses (ICD-10).
 */
public interface MedicalRecordDiagnosisRepository {

    List<MedicalRecordDiagnosis> findByMedicalRecordId(UUID medicalRecordId);
}
