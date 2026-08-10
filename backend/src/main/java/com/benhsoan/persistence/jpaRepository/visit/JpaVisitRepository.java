package com.benhsoan.persistence.jpaRepository.visit;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;
import com.benhsoan.persistence.entity.auth.UserEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.entity.patient.PatientEntity;
import com.benhsoan.persistence.entity.queue.MedicalQueueEntity;
import com.benhsoan.persistence.entity.queue.QueueItemEntity;
import com.benhsoan.persistence.entity.queue.RoomEntity;
import com.benhsoan.domain.visit.enums.VisitStatus;

public interface JpaVisitRepository extends JpaRepository<VisitEntity, UUID> {

    Optional<VisitEntity> findByVisitCode(String visitCode);

    Optional<VisitEntity> findTopByOrderByVisitCodeDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select visit from VisitEntity visit where visit.id = :visitId")
    Optional<VisitEntity> findByIdForUpdate(@Param("visitId") UUID visitId);

    List<VisitEntity> findByPatientIdOrderByVisitAtDesc(UUID patientId);

    boolean existsByPatientIdAndStatusIn(UUID patientId, Collection<VisitStatus> statuses);

    boolean existsByPatientIdAndStatusInAndVisitAtBetween(
            UUID patientId,
            Collection<VisitStatus> statuses,
            Instant fromInclusive,
            Instant toExclusive
    );

    @Query("""
            select new com.benhsoan.persistence.jpaRepository.visit.VisitEncounterProjection(
                visit.id, visit.visitCode, visit.visitType, visit.status, visit.visitAt, visit.startedAt,
                visit.reason, visit.note,
                patient.id, patient.patientCode, patient.fullName, patient.dateOfBirth, patient.gender, patient.phone,
                doctor.id, doctor.fullName,
                room.id, room.code,
                queueItem.id, queueItem.queueNumber, queueItem.status, queueItem.checkedInAt, queueItem.calledAt,
                appointment.id, appointment.appointmentCode, appointment.status,
                medicalRecord.id, medicalRecord.status, medicalRecord.lockedAt
            )
            from VisitEntity visit
            join PatientEntity patient on patient.id = visit.patientId
            join UserEntity doctor on doctor.id = visit.doctorId
            left join QueueItemEntity queueItem on queueItem.visitId = visit.id
            left join MedicalQueueEntity medicalQueue on medicalQueue.id = queueItem.medicalQueueId
            left join RoomEntity room on room.id = medicalQueue.roomId
            left join AppointmentEntity appointment on appointment.id = visit.appointmentId
            left join MedicalRecordEntity medicalRecord on medicalRecord.visitId = visit.id
            where visit.id = :visitId
            """)
    Optional<VisitEncounterProjection> findEncounterById(@Param("visitId") UUID visitId);
}
