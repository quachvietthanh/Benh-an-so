package com.benhsoan.persistence.jpaRepository.appointment;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.appointment.DoctorWeeklyScheduleEntity;

public interface JpaDoctorWeeklyScheduleRepository extends JpaRepository<DoctorWeeklyScheduleEntity, UUID> {

    List<DoctorWeeklyScheduleEntity> findByDoctorId(UUID doctorId);

    List<DoctorWeeklyScheduleEntity> findByDoctorIdAndActiveTrue(UUID doctorId);

    Optional<DoctorWeeklyScheduleEntity> findByDoctorIdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek);

}
