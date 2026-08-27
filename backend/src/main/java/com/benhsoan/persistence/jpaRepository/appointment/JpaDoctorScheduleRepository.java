package com.benhsoan.persistence.jpaRepository.appointment;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.appointment.DoctorScheduleEntity;

import jakarta.persistence.LockModeType;

public interface JpaDoctorScheduleRepository extends JpaRepository<DoctorScheduleEntity, UUID> {

    Optional<DoctorScheduleEntity> findByDoctorIdAndScheduleDate(UUID doctorId, LocalDate scheduleDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select schedule from DoctorScheduleEntity schedule "
            + "where schedule.doctorId = :doctorId and schedule.scheduleDate = :scheduleDate")
    Optional<DoctorScheduleEntity> findByDoctorIdAndScheduleDateForUpdate(
            @Param("doctorId") UUID doctorId,
            @Param("scheduleDate") LocalDate scheduleDate
    );

}
