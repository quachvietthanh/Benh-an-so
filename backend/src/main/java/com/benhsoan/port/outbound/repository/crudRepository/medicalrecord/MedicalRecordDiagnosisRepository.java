package com.benhsoan.port.outbound.repository.crudRepository.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;

public interface MedicalRecordDiagnosisRepository {

    List<MedicalRecordDiagnosis> findByMedicalRecordId(UUID medicalRecordId);

    List<MedicalRecordDiagnosis> replaceForMedicalRecord(
            UUID medicalRecordId,
            List<MedicalRecordDiagnosis> diagnoses
    );
}
