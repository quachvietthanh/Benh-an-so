package com.benhsoan.persistence.jpaRepository.appointment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.Collection;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.benhsoan.persistence.entity.appointment.AppointmentEntity;

public interface JpaAppointmentRepository
        extends JpaRepository<AppointmentEntity, UUID>,
                JpaSpecificationExecutor<AppointmentEntity> {

    Optional<AppointmentEntity> findByAppointmentCode(String appointmentCode);

    boolean existsByAppointmentCode(String appointmentCode);

    List<AppointmentEntity> findByDoctorId(UUID doctorId);

    List<AppointmentEntity> findByPatientId(UUID patientId);

    Optional<AppointmentEntity> findTopByOrderByAppointmentCodeDesc();

    @Query(value = """
            SELECT appointment_code
            FROM appointments
            WHERE appointment_code REGEXP '[0-9]{6}$'
            ORDER BY CAST(RIGHT(appointment_code, 6) AS UNSIGNED) DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findAppointmentCodeWithHighestSequence();

    @Query("select appointment.id from AppointmentEntity appointment "
            + "where appointment.startTime > :now "
            + "and appointment.startTime <= :reminderDeadline "
            + "and appointment.status in :statuses")
    List<UUID> findDueReminderIds(
            @Param("now") Instant now,
            @Param("reminderDeadline") Instant reminderDeadline,
            @Param("statuses") Collection<AppointmentStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from AppointmentEntity appointment where appointment.id = :id")
    Optional<AppointmentEntity> findByIdForUpdate(@Param("id") UUID id);
}
