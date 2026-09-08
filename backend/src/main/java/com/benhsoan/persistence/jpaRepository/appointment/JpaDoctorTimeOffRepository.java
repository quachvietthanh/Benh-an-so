package com.benhsoan.persistence.jpaRepository.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.appointment.enums.TimeOffStatus;
import com.benhsoan.persistence.entity.appointment.DoctorTimeOffEntity;

public interface JpaDoctorTimeOffRepository extends JpaRepository<DoctorTimeOffEntity, UUID> {

    @Query("""
        SELECT dto FROM DoctorTimeOffEntity dto
        WHERE dto.doctorId = :doctorId
          AND dto.status = :status
          AND dto.startTime < :endTime
          AND dto.endTime > :startTime
        ORDER BY dto.startTime ASC
    """)
    List<DoctorTimeOffEntity> findTimeOffsOverlapping(
            @Param("doctorId") UUID doctorId,
            @Param("status") TimeOffStatus status,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("""
        SELECT dto FROM DoctorTimeOffEntity dto
        WHERE (:doctorId IS NULL OR dto.doctorId = :doctorId)
          AND (:status IS NULL OR dto.status = :status)
          AND (:fromTime IS NULL OR dto.endTime >= :fromTime)
          AND (:toTime IS NULL OR dto.startTime <= :toTime)
        ORDER BY dto.startTime ASC
    """)
    List<DoctorTimeOffEntity> search(
            @Param("doctorId") UUID doctorId,
            @Param("status") TimeOffStatus status,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime
    );

}
