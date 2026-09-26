package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class AppointmentAlreadyInProgressException extends AppointmentException {

    public AppointmentAlreadyInProgressException() {
        super(DomainErrorCode.APPOINTMENT_ALREADY_IN_PROGRESS,
                "Appointment has already been in progress."
        );
    }

}
