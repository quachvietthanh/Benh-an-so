package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class UnauthorizedAppointmentOperationException extends AppointmentException {

    public UnauthorizedAppointmentOperationException() {
        super(DomainErrorCode.UNAUTHORIZED_APPOINTMENT_OPERATION,
                "You are not authorized to perform this appointment operation."
        );
    }

}
