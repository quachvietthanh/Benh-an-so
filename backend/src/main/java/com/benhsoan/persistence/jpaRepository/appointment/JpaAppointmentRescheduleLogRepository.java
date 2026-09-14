package com.benhsoan.persistence.jpaRepository.appointment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.appointment.AppointmentRescheduleLogEntity;

public interface JpaAppointmentRescheduleLogRepository
        extends JpaRepository<AppointmentRescheduleLogEntity, UUID> {

    List<AppointmentRescheduleLogEntity> findByAppointmentIdOrderByRescheduledAtDesc(UUID appointmentId);

}
