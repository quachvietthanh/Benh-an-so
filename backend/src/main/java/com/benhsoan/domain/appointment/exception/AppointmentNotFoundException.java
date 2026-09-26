package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class AppointmentNotFoundException extends AppointmentException {

    public AppointmentNotFoundException(UUID appointmentId) {
        super(DomainErrorCode.APPOINTMENT_NOT_FOUND,
                "Appointment not found with id: " + appointmentId
        );
    }

}
