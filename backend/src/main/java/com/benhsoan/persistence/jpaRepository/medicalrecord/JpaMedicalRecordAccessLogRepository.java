package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;

public interface JpaMedicalRecordAccessLogRepository
        extends JpaRepository<MedicalRecordAccessLogEntity, UUID> {

    @Query("""
            select accessLog from MedicalRecordAccessLogEntity accessLog
            where accessLog.medicalRecordId = :medicalRecordId
              and (:from is null or accessLog.accessedAt >= :from)
              and (:to is null or accessLog.accessedAt <= :to)
            order by accessLog.accessedAt desc
            """)
    Page<MedicalRecordAccessLogEntity> findByMedicalRecordId(
            @Param("medicalRecordId") UUID medicalRecordId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query("""
            select accessLog from MedicalRecordAccessLogEntity accessLog
            where accessLog.patientId = :patientId
              and (:from is null or accessLog.accessedAt >= :from)
              and (:to is null or accessLog.accessedAt <= :to)
            order by accessLog.accessedAt desc
            """)
    Page<MedicalRecordAccessLogEntity> findByPatientId(
            @Param("patientId") UUID patientId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
