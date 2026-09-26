package com.benhsoan.persistence.jpaRepository.patient;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.patient.PatientImportLogEntity;

public interface JpaPatientImportLogRepository extends JpaRepository<PatientImportLogEntity, UUID> {
    Page<PatientImportLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
