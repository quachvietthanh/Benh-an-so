package com.benhsoan.domain.appointment.exception;


public class UnauthorizedAppointmentOperationException extends AppointmentException {

    public UnauthorizedAppointmentOperationException() {
        super(
                "You are not authorized to perform this appointment operation."
        );
    }

}
