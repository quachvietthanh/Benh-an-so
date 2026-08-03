package com.benhsoan.persistence.adapterRepository.visit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;
import com.benhsoan.persistence.jpaRepository.visit.VisitEncounterProjection;
import com.benhsoan.port.dto.result.VisitEncounterResult;
import com.benhsoan.port.outbound.repository.queryRepository.visit.VisitEncounterQueryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VisitEncounterQueryRepositoryAdapter implements VisitEncounterQueryRepository {

    private final JpaVisitRepository jpaVisitRepository;

    @Override
    public Optional<VisitEncounterResult> findByVisitId(UUID visitId) {
        return jpaVisitRepository.findEncounterById(visitId).map(this::toResult);
    }

    private VisitEncounterResult toResult(VisitEncounterProjection projection) {
        VisitEncounterResult.RoomInfo room = projection.roomId() == null
                ? null
                : new VisitEncounterResult.RoomInfo(projection.roomId(), projection.roomNumber());
        VisitEncounterResult.QueueItemInfo queueItem = projection.queueItemId() == null
                ? null
                : new VisitEncounterResult.QueueItemInfo(
                        projection.queueItemId(), projection.queueNumber(), projection.queueItemStatus(),
                        projection.checkedInAt(), projection.calledAt());
        VisitEncounterResult.AppointmentInfo appointment = projection.appointmentId() == null
                ? null
                : new VisitEncounterResult.AppointmentInfo(
                        projection.appointmentId(), projection.appointmentCode(), projection.appointmentStatus());
        VisitEncounterResult.MedicalRecordInfo medicalRecord = projection.medicalRecordId() == null
                ? null
                : new VisitEncounterResult.MedicalRecordInfo(
                        projection.medicalRecordId(), projection.medicalRecordStatus(), projection.medicalRecordLockedAt());

        return new VisitEncounterResult(
                new VisitEncounterResult.VisitInfo(
                        projection.visitId(), projection.visitCode(), projection.visitType(), projection.visitStatus(),
                        projection.visitAt(), projection.startedAt(), projection.reason(), projection.note()),
                new VisitEncounterResult.PatientInfo(
                        projection.patientId(), projection.patientCode(), projection.patientName(),
                        projection.patientDateOfBirth(), projection.patientGender(), projection.patientPhone()),
                new VisitEncounterResult.DoctorInfo(projection.doctorId(), projection.doctorName()),
                room,
                queueItem,
                appointment,
                medicalRecord);
    }
}
