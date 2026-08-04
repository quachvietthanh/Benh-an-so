package com.benhsoan.port.outbound.repository.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;

public interface MedicalRecordAccessLogRepository {

    MedicalRecordAccessLog save(MedicalRecordAccessLog accessLog);

    Page<MedicalRecordAccessLog> findByMedicalRecordId(
            UUID medicalRecordId,
            Instant from,
            Instant to,
            Pageable pageable
    );

    Page<MedicalRecordAccessLog> findByPatientId(
            UUID patientId,
            Instant from,
            Instant to,
            Pageable pageable
    );
}
