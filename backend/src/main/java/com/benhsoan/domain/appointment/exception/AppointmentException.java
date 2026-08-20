package com.benhsoan.domain.appointment.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class AppointmentException extends DomainException {

    protected AppointmentException(
            String message
    ) {
        super(message);
    }

}
