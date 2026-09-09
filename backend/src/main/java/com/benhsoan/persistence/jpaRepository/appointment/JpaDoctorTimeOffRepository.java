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

    List<DoctorTimeOffEntity> findByDoctorIdOrderByStartTimeDesc(UUID doctorId);

    List<DoctorTimeOffEntity> findByDoctorIdAndStatusOrderByStartTimeAsc(UUID doctorId, TimeOffStatus status);

    @Query("select t from DoctorTimeOffEntity t "
            + "where t.doctorId = :doctorId and t.status = :status "
            + "and t.startTime < :endTime and t.endTime > :startTime order by t.startTime asc")
    List<DoctorTimeOffEntity> findOverlappingTimeOffs(
            @Param("doctorId") UUID doctorId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("status") TimeOffStatus status
    );

    @Query("select count(t) > 0 from DoctorTimeOffEntity t "
            + "where t.doctorId = :doctorId and t.status = :status "
            + "and t.startTime < :endTime and t.endTime > :startTime")
    boolean existsOverlapping(
            @Param("doctorId") UUID doctorId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("status") TimeOffStatus status
    );
}
