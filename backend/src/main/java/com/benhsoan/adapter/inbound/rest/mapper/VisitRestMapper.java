package com.benhsoan.adapter.inbound.rest.mapper;

import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.visit.VisitEncounterResponse;
import com.benhsoan.domain.patient.PatientAnonymizer;
import com.benhsoan.port.dto.result.VisitEncounterResult;

@Component
public class VisitRestMapper {

    private final AnonymizationModeState anonymizationModeState;

    public VisitRestMapper(
            AnonymizationModeState anonymizationModeState) {
        this.anonymizationModeState = anonymizationModeState;
    }

    public VisitEncounterResponse toResponse(VisitEncounterResult result) {
        return new VisitEncounterResponse(
                new VisitEncounterResponse.VisitInfo(
                        result.visit().id(), result.visit().visitCode(), result.visit().type(), result.visit().status(),
                        result.visit().visitAt(), result.visit().startedAt(), result.visit().reason(), result.visit().note()),
                new VisitEncounterResponse.PatientInfo(
                        result.patient().id(), result.patient().patientCode(),
                        anonymizationModeState.isEnabled() ? PatientAnonymizer.maskFullName(result.patient().patientCode()) : result.patient().fullName(),
                        result.patient().dateOfBirth(), result.patient().gender(),
                        anonymizationModeState.isEnabled() ? PatientAnonymizer.maskPhone(result.patient().phone()) : result.patient().phone()),
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

    public com.benhsoan.adapter.inbound.rest.response.visit.VisitHandoverResponse toResponse(
            com.benhsoan.port.dto.result.VisitHandoverResult result) {
        if (result == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.visit.VisitHandoverResponse(
                result.id(),
                result.visitId(),
                result.fromDoctorId(),
                result.fromDoctorName(),
                result.toDoctorId(),
                result.toDoctorName(),
                result.reason(),
                result.handedOverAt()
        );
    }

    public com.benhsoan.adapter.inbound.rest.response.visit.HandoverDoctorResponse toHandoverDoctorResponse(
            com.benhsoan.port.dto.result.UserResult result) {
        if (result == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.visit.HandoverDoctorResponse(
                result.id(),
                result.fullName()
        );
    }

    public java.util.List<com.benhsoan.adapter.inbound.rest.response.visit.HandoverDoctorResponse> toHandoverDoctorResponseList(
            java.util.List<com.benhsoan.port.dto.result.UserResult> results) {
        if (results == null) {
            return java.util.Collections.emptyList();
        }
        return results.stream()
                .map(this::toHandoverDoctorResponse)
                .toList();
    }

    public com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse toResponse(
            com.benhsoan.port.dto.result.VisitSummaryResult result) {
        if (result == null) return null;

        var clinic = result.clinic() != null
                ? new com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse.ClinicInfo(
                        result.clinic().name(), result.clinic().address(), result.clinic().phone())
                : null;

        var patient = result.patient() != null
                ? new com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse.PatientInfo(
                        result.patient().id(),
                        result.patient().patientCode(),
                        anonymizationModeState.isEnabled()
                                ? PatientAnonymizer.maskFullName(result.patient().patientCode())
                                : result.patient().fullName(),
                        result.patient().dateOfBirth(),
                        result.patient().gender(),
                        anonymizationModeState.isEnabled()
                                ? PatientAnonymizer.maskPhone(result.patient().phone())
                                : result.patient().phone(),
                        result.patient().identityNumber())
                : null;

        var doctor = result.doctor() != null
                ? new com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse.DoctorInfo(
                        result.doctor().id(), result.doctor().fullName())
                : null;

        var medicalRecord = result.medicalRecord() != null
                ? new com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse.MedicalRecordInfo(
                        result.medicalRecord().id(),
                        result.medicalRecord().status(),
                        result.medicalRecord().signedAt(),
                        result.medicalRecord().signedBy(),
                        result.medicalRecord().signedByName())
                : null;

        var diagnoses = result.diagnoses() != null
                ? result.diagnoses().stream()
                        .map(d -> new com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse.DiagnosisItem(
                                d.code(), d.name(), d.isPrimary()))
                        .toList()
                : java.util.Collections.<com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse.DiagnosisItem>emptyList();

        var clinicalOrders = result.clinicalOrders() != null
                ? result.clinicalOrders().stream()
                        .map(o -> new com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse.ClinicalOrderItemInfo(
                                o.orderCode(), o.serviceCode(), o.serviceName(), o.instruction(), o.status()))
                        .toList()
                : java.util.Collections.<com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse.ClinicalOrderItemInfo>emptyList();

        var printHistory = result.printHistory() != null
                ? result.printHistory().stream()
                        .map(p -> new com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse.PrintHistoryItem(
                                p.printedBy(), p.printedByName(), p.printedAt(), p.detail()))
                        .toList()
                : java.util.Collections.<com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse.PrintHistoryItem>emptyList();

        return new com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse(
                result.visitId(),
                result.visitCode(),
                result.visitAt(),
                clinic,
                patient,
                doctor,
                medicalRecord,
                diagnoses,
                clinicalOrders,
                result.doctorInstructions(),
                result.treatmentPlan(),
                result.revisitDate(),
                printHistory
        );
    }
}
