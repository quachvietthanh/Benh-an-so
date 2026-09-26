package com.benhsoan.domain.appointment.exception;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * The doctor's working schedule for the requested date is inactive (NCL-14-CN-003).
 */
public class DoctorUnavailableException extends AppointmentException {

    public DoctorUnavailableException(UUID doctorId, LocalDate date) {
        super(DomainErrorCode.DOCTOR_SCHEDULE_UNAVAILABLE,
                "Bác sĩ không khả dụng vào ngày " + date + "."
        );
    }

}
