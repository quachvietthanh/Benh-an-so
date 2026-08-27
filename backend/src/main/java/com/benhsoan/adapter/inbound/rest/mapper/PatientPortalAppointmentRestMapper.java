package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.appointment.PatientBookAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.PatientCancelAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.request.appointment.PatientRescheduleAppointmentRequest;
import com.benhsoan.adapter.inbound.rest.response.appointment.PatientAppointmentResponse;
import com.benhsoan.port.dto.command.appointment.PatientBookAppointmentCommand;
import com.benhsoan.port.dto.command.appointment.PatientCancelAppointmentCommand;
import com.benhsoan.port.dto.command.appointment.PatientRescheduleAppointmentCommand;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;

@Component
public class PatientPortalAppointmentRestMapper {

    public PatientBookAppointmentCommand toCommand(PatientBookAppointmentRequest request) {
        return new PatientBookAppointmentCommand(
                request.doctorId(),
                request.appointmentDate(),
                request.startTime(),
                request.reason()
        );
    }

    public PatientCancelAppointmentCommand toCommand(PatientCancelAppointmentRequest request) {
        return new PatientCancelAppointmentCommand(
                request.cancellationReason()
        );
    }

    public PatientRescheduleAppointmentCommand toCommand(PatientRescheduleAppointmentRequest request) {
        return new PatientRescheduleAppointmentCommand(
                request.newAppointmentDate(),
                request.newStartTime(),
                request.reason()
        );
    }

    public PatientAppointmentResponse toResponse(PatientAppointmentResult result) {
        return new PatientAppointmentResponse(
                result.id(),
                result.appointmentCode(),
                result.patientId(),
                result.doctorId(),
                result.startTime(),
                result.endTime(),
                result.status(),
                result.reason(),
                result.bookingChannel(),
                result.createdAt()
        );
    }

}
