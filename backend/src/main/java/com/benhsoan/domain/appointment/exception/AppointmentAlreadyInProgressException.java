package com.benhsoan.domain.appointment.exception;


public class AppointmentAlreadyInProgressException extends AppointmentException {

    public AppointmentAlreadyInProgressException() {
        super(
                "Appointment has already been in progress."
        );
    }

}
