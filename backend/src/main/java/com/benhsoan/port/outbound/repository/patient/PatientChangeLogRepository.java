package com.benhsoan.port.outbound.repository.patient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.patient.PatientChangeLog;

public interface PatientChangeLogRepository {

    PatientChangeLog save(PatientChangeLog log);

    Optional<PatientChangeLog> findById(UUID id);

    Page<PatientChangeLog> findAll(Pageable pageable);

    Page<PatientChangeLog> findByPatientId(
            UUID patientId,
            Pageable pageable
    );

}
