package com.benhsoan.port.outbound.repository.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;

public interface MedicalRecordDiagnosisRepository {

    boolean existsByMedicalRecordId(UUID medicalRecordId);

    List<MedicalRecordDiagnosis> findByMedicalRecordId(UUID medicalRecordId);

    List<MedicalRecordDiagnosis> replaceForMedicalRecord(
            UUID medicalRecordId,
            List<MedicalRecordDiagnosis> diagnoses
    );
}
