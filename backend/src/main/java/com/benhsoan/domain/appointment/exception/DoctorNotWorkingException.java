package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when an appointment is booked or rescheduled outside the doctor's working schedule
 * or during a registered time-off / leave interval (QTN-30 / NCL-03-CN-006 TC-02).
 */
public class DoctorNotWorkingException extends AppointmentException {

    private static final String DEFAULT_MESSAGE = "Bác sĩ không làm việc trong khung giờ này.";

    public DoctorNotWorkingException() {
        super(DomainErrorCode.DOCTOR_NOT_WORKING, DEFAULT_MESSAGE);
    }

    public DoctorNotWorkingException(String message) {
        super(DomainErrorCode.DOCTOR_NOT_WORKING, message != null ? message : DEFAULT_MESSAGE);
    }
}
