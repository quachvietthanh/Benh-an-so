package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * The requested appointment time is in the past or before the clinic buffer (TC-03).
 */
public class InvalidAppointmentTimeException extends AppointmentException {

    public InvalidAppointmentTimeException() {
        super(DomainErrorCode.APPOINTMENT_TIME_IN_PAST,
                "Không thể đặt lịch hẹn trong quá khứ."
        );
    }

}
