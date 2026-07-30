package com.benhsoan.port.outbound.repository.crudRepository.medicalrecord;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface MedicalRecordRepository extends BaseRepository<MedicalRecord, UUID> {

    Optional<MedicalRecord> findByVisitId(UUID visitId);

    boolean existsByVisitId(UUID visitId);
}
