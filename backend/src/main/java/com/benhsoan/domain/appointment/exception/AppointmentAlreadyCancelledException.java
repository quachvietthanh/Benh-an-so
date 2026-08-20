package com.benhsoan.domain.appointment.exception;


public class AppointmentAlreadyCancelledException extends AppointmentException {

    public AppointmentAlreadyCancelledException() {
        super(
                "Appointment has already been cancelled."
        );
    }

}
