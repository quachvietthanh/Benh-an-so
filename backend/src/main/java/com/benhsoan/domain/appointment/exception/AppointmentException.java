package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class AppointmentException extends DomainException {

    protected AppointmentException(
            DomainErrorCode code,
            String message
    ) {
        super(code, message);
    }

}
