package com.benhsoan.domain.appointment.exception;


public class AppointmentNotOverdueException extends AppointmentException {

    public AppointmentNotOverdueException() {
        super(
                "Appointment has not exceeded the no-show threshold."
        );
    }

}
