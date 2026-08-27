package com.benhsoan.application.ucservice.appointment;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;

@Component
public class PatientAppointmentResultMapper {

    public PatientAppointmentResult toResult(Appointment appointment) {
        return new PatientAppointmentResult(
                appointment.getId(),
                appointment.getAppointmentCode(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getBookingChannel(),
                appointment.getCreatedAt()
        );
    }
}
