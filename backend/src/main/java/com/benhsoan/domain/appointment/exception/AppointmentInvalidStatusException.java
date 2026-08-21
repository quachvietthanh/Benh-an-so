package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class AppointmentInvalidStatusException extends AppointmentException {

    public AppointmentInvalidStatusException(String message) {
        super(DomainErrorCode.APPOINTMENT_INVALID_STATUS, message);
    }

}
