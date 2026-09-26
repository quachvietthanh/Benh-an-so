package com.benhsoan.port.outbound.repository.medicalrecord;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;

public interface MedicalRecordDiagnosisRepository {

    boolean existsByMedicalRecordId(UUID medicalRecordId);

    boolean existsByDiagnosisCatalogId(UUID diagnosisCatalogId);

    boolean existsByMedicalRecordIdAndDiagnosisCatalogId(
            UUID medicalRecordId,
            UUID diagnosisCatalogId
    );

    List<MedicalRecordDiagnosis> findByMedicalRecordId(UUID medicalRecordId);

    List<MedicalRecordDiagnosis> findByMedicalRecordIdAndDiagnosisType(
            UUID medicalRecordId,
            com.benhsoan.domain.medicalrecord.enums.DiagnosisType diagnosisType
    );

    List<MedicalRecordDiagnosis> findByMedicalRecordIdIn(Collection<UUID> medicalRecordIds);

    List<MedicalRecordDiagnosis> replaceForMedicalRecord(
            UUID medicalRecordId,
            List<MedicalRecordDiagnosis> diagnoses
    );

    List<UUID> findRecentCatalogIdsByDoctor(UUID doctorId, int limit);

    List<UUID> findPopularCatalogIdsBySpecialty(UUID specialtyId, int limit);
}
