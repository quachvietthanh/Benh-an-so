package com.benhsoan.domain.appointment.exception;


public class AppointmentTimeInPastException extends AppointmentException {

    public AppointmentTimeInPastException() {
        super(
                "Appointment time cannot be in the past."
        );
    }

}
