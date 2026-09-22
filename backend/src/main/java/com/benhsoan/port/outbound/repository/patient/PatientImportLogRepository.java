package com.benhsoan.port.outbound.repository.patient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.patient.PatientImportLog;

public interface PatientImportLogRepository {
    PatientImportLog save(PatientImportLog log);
    Optional<PatientImportLog> findById(UUID id);
    Page<PatientImportLog> findAll(Pageable pageable);
}
