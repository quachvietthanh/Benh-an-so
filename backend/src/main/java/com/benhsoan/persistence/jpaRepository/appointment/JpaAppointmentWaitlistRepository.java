package com.benhsoan.persistence.jpaRepository.appointment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.appointment.enums.WaitlistStatus;
import com.benhsoan.persistence.entity.appointment.AppointmentWaitlistEntity;

import jakarta.persistence.LockModeType;

public interface JpaAppointmentWaitlistRepository extends JpaRepository<AppointmentWaitlistEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM AppointmentWaitlistEntity w WHERE w.id = :id")
    Optional<AppointmentWaitlistEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<AppointmentWaitlistEntity> findFirstByDoctorIdAndDesiredDateAndStatusOrderByCreatedAtAsc(
            UUID doctorId,
            LocalDate desiredDate,
            WaitlistStatus status
    );

    @Query("""
        SELECT w FROM AppointmentWaitlistEntity w
        WHERE (:doctorId IS NULL OR w.doctorId = :doctorId)
          AND (:desiredDate IS NULL OR w.desiredDate = :desiredDate)
          AND (:fromDate IS NULL OR w.desiredDate >= :fromDate)
          AND (:status IS NULL OR w.status = :status)
        ORDER BY w.createdAt ASC
    """)
    List<AppointmentWaitlistEntity> findWaitlist(
            @Param("doctorId") UUID doctorId,
            @Param("desiredDate") LocalDate desiredDate,
            @Param("fromDate") LocalDate fromDate,
            @Param("status") WaitlistStatus status
    );


    boolean existsByPatientIdAndDoctorIdAndDesiredDateAndStatus(
            UUID patientId,
            UUID doctorId,
            LocalDate desiredDate,
            WaitlistStatus status
    );

    Optional<AppointmentWaitlistEntity> findFirstByPatientIdAndDoctorIdAndDesiredDateAndStatusOrderByCreatedAtAsc(
            UUID patientId,
            UUID doctorId,
            LocalDate desiredDate,
            WaitlistStatus status
    );
}
