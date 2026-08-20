package com.benhsoan.domain.appointment.exception;


public class AppointmentTimeConflictException extends AppointmentException {

    public AppointmentTimeConflictException() {
        super(
                "Doctor already has an appointment during the selected time."
        );
    }

}
