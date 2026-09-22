package com.benhsoan.persistence.jpaRepository.patient;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.patient.PatientImportLogErrorEntity;

public interface JpaPatientImportLogErrorRepository extends JpaRepository<PatientImportLogErrorEntity, UUID> {
    List<PatientImportLogErrorEntity> findByImportLogIdOrderByRowNumberAsc(UUID importLogId);
}
