package com.benhsoan.port.outbound.repository.medicalrecord;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
public interface MedicalRecordRepository {

    Optional<MedicalRecord> findById(UUID id);

    Optional<MedicalRecord> findByIdForUpdate(UUID id);

    MedicalRecord save(MedicalRecord medicalRecord);

    Optional<MedicalRecord> findByVisitId(UUID visitId);

    boolean existsByVisitId(UUID visitId);

    void deleteById(UUID id);
}
