package com.benhsoan.persistence.jpaRepository.visit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;

public record VisitEncounterProjection(
        UUID visitId,
        String visitCode,
        VisitType visitType,
        VisitStatus visitStatus,
        Instant visitAt,
        Instant startedAt,
        String reason,
        String note,
        UUID patientId,
        String patientCode,
        String patientName,
        LocalDate patientDateOfBirth,
        Gender patientGender,
        String patientPhone,
        UUID doctorId,
        String doctorName,
        UUID roomId,
        String roomNumber,
        UUID queueItemId,
        Integer queueNumber,
        QueueItemStatus queueItemStatus,
        Instant checkedInAt,
        Instant calledAt,
        UUID appointmentId,
        String appointmentCode,
        AppointmentStatus appointmentStatus,
        UUID medicalRecordId,
        MedicalRecordStatus medicalRecordStatus,
        Instant medicalRecordLockedAt
) {
}
