package com.benhsoan.domain.appointment.exception;

import java.util.UUID;


public class AppointmentNotFoundException extends AppointmentException {

    public AppointmentNotFoundException(UUID appointmentId) {
        super(
                "Appointment not found with id: " + appointmentId
        );
    }

}
