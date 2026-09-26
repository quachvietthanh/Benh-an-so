package com.benhsoan.adapter.inbound.rest.response.visit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;

public record VisitEncounterResponse(
        VisitInfo visit,
        PatientInfo patient,
        DoctorInfo doctor,
        RoomInfo room,
        QueueItemInfo queueItem,
        AppointmentInfo appointment,
        MedicalRecordInfo medicalRecord
) {

    public record VisitInfo(
            UUID id,
            String visitCode,
            VisitType type,
            VisitStatus status,
            Instant visitAt,
            Instant startedAt,
            String reason,
            String note
    ) {
    }

    public record PatientInfo(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone
    ) {
    }

    public record DoctorInfo(UUID id, String fullName) {
    }

    public record RoomInfo(UUID id, String roomNumber) {
    }

    public record QueueItemInfo(
            UUID id,
            int queueNumber,
            QueueItemStatus status,
            Instant checkedInAt,
            Instant calledAt
    ) {
    }

    public record AppointmentInfo(UUID id, String appointmentCode, AppointmentStatus status) {
    }

    public record MedicalRecordInfo(UUID id, MedicalRecordStatus status, Instant lockedAt) {
    }
}
