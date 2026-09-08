package com.benhsoan.persistence.jpaRepository.appointment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.appointment.DoctorWeeklyScheduleEntity;

public interface JpaDoctorWeeklyScheduleRepository extends JpaRepository<DoctorWeeklyScheduleEntity, UUID> {

    List<DoctorWeeklyScheduleEntity> findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(UUID doctorId);

    List<DoctorWeeklyScheduleEntity> findByDoctorIdAndDayOfWeek(UUID doctorId, int dayOfWeek);

    void deleteByDoctorId(UUID doctorId);

}
