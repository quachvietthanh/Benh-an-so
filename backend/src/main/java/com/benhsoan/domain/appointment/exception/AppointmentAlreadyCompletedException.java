package com.benhsoan.domain.appointment.exception;


public class AppointmentAlreadyCompletedException extends AppointmentException {

    public AppointmentAlreadyCompletedException() {
        super(
                "Appointment has already been completed."
        );
    }

}
