package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class AppointmentNotOverdueException extends AppointmentException {

    public AppointmentNotOverdueException() {
        super(DomainErrorCode.APPOINTMENT_NOT_OVERDUE,
                "Appointment has not exceeded the no-show threshold."
        );
    }

}
