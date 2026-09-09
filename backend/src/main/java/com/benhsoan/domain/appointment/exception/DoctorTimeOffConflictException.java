package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when registering a doctor time-off interval that overlaps with an existing active time-off.
 */
public class DoctorTimeOffConflictException extends AppointmentException {

    public DoctorTimeOffConflictException() {
        super(DomainErrorCode.DOCTOR_TIMEOFF_CONFLICT, "Khoảng thời gian nghỉ trùng lặp với khoảng nghỉ đã đăng ký.");
    }

    public DoctorTimeOffConflictException(String message) {
        super(DomainErrorCode.DOCTOR_TIMEOFF_CONFLICT, message);
    }
}
