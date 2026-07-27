package com.benhsoan.domain.appointment.exception;

import org.springframework.http.HttpStatus;

public class AppointmentNotOverdueException extends AppointmentException {

    public AppointmentNotOverdueException() {
        super(
                HttpStatus.CONFLICT,
                "Appointment has not exceeded the no-show threshold."
        );
    }

}