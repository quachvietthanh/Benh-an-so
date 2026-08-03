package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.visit.VisitEncounterResponse;
import com.benhsoan.port.dto.result.VisitEncounterResult;

@Component
public class VisitRestMapper {

    public VisitEncounterResponse toResponse(VisitEncounterResult result) {
        return new VisitEncounterResponse(
                new VisitEncounterResponse.VisitInfo(
                        result.visit().id(), result.visit().visitCode(), result.visit().type(), result.visit().status(),
                        result.visit().visitAt(), result.visit().startedAt(), result.visit().reason(), result.visit().note()),
                new VisitEncounterResponse.PatientInfo(
                        result.patient().id(), result.patient().patientCode(), result.patient().fullName(),
                        result.patient().dateOfBirth(), result.patient().gender(), result.patient().phone()),
                new VisitEncounterResponse.DoctorInfo(result.doctor().id(), result.doctor().fullName()),
                toRoomResponse(result.room()),
                toQueueItemResponse(result.queueItem()),
                toAppointmentResponse(result.appointment()),
                toMedicalRecordResponse(result.medicalRecord()));
    }

    private VisitEncounterResponse.RoomInfo toRoomResponse(VisitEncounterResult.RoomInfo room) {
        return room == null ? null : new VisitEncounterResponse.RoomInfo(room.id(), room.roomNumber());
    }

    private VisitEncounterResponse.QueueItemInfo toQueueItemResponse(VisitEncounterResult.QueueItemInfo queueItem) {
        return queueItem == null
                ? null
                : new VisitEncounterResponse.QueueItemInfo(
                        queueItem.id(), queueItem.queueNumber(), queueItem.status(),
                        queueItem.checkedInAt(), queueItem.calledAt());
    }

    private VisitEncounterResponse.AppointmentInfo toAppointmentResponse(
            VisitEncounterResult.AppointmentInfo appointment) {
        return appointment == null
                ? null
                : new VisitEncounterResponse.AppointmentInfo(
                        appointment.id(), appointment.appointmentCode(), appointment.status());
    }

    private VisitEncounterResponse.MedicalRecordInfo toMedicalRecordResponse(
            VisitEncounterResult.MedicalRecordInfo medicalRecord) {
        return medicalRecord == null
                ? null
                : new VisitEncounterResponse.MedicalRecordInfo(
                        medicalRecord.id(), medicalRecord.status(), medicalRecord.lockedAt());
    }
}
