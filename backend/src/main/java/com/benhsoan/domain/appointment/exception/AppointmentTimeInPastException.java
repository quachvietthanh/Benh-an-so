package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class AppointmentTimeInPastException extends AppointmentException {

    public AppointmentTimeInPastException() {
        super(DomainErrorCode.APPOINTMENT_TIME_IN_PAST,
                "Appointment time cannot be in the past."
        );
    }

}
