package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class AppointmentAlreadyCancelledException extends AppointmentException {

    public AppointmentAlreadyCancelledException() {
        super(DomainErrorCode.APPOINTMENT_ALREADY_CANCELLED,
                "Appointment has already been cancelled."
        );
    }

}
