package com.benhsoan.persistence.jpaRepository.medicalrecord;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;

public interface JpaMedicalRecordAccessLogRepository
        extends JpaRepository<MedicalRecordAccessLogEntity, UUID>,
        JpaSpecificationExecutor<MedicalRecordAccessLogEntity> {

    @Modifying
    @Query("delete from MedicalRecordAccessLogEntity log where log.medicalRecordId = :medicalRecordId")
    void deleteByMedicalRecordId(@Param("medicalRecordId") UUID medicalRecordId);

    List<MedicalRecordAccessLogEntity> findByActionAndAccessedAtBetween(
            MedicalRecordAccessAction action,
            Instant from,
            Instant to
    );

    @Query("select log.accessedBy as accessedBy, count(log) as accessCount "
            + "from MedicalRecordAccessLogEntity log "
            + "where log.action in :actions "
            + "and log.accessedAt >= :from "
            + "and log.accessedAt < :to "
            + "group by log.accessedBy "
            + "order by count(log) desc, log.accessedBy asc")
    List<AccessLogAccountCountProjection> countAccessByAccount(
            @Param("actions") Collection<MedicalRecordAccessAction> actions,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
