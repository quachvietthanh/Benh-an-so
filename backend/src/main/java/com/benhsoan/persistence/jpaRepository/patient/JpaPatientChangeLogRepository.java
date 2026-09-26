package com.benhsoan.persistence.jpaRepository.patient;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.patient.PatientChangeLogEntity;

public interface JpaPatientChangeLogRepository
        extends JpaRepository<PatientChangeLogEntity, UUID> {

    Page<PatientChangeLogEntity> findByPatientId(
            UUID patientId,
            Pageable pageable
    );

}