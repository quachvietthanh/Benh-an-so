package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class AppointmentAlreadyCompletedException extends AppointmentException {

    public AppointmentAlreadyCompletedException() {
        super(DomainErrorCode.APPOINTMENT_ALREADY_COMPLETED,
                "Appointment has already been completed."
        );
    }

}
