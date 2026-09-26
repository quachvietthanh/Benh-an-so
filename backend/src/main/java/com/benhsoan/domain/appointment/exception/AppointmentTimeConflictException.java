package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class AppointmentTimeConflictException extends AppointmentException {

    public AppointmentTimeConflictException() {
        super(DomainErrorCode.APPOINTMENT_TIME_CONFLICT,
                "Doctor already has an appointment during the selected time."
        );
    }

}
