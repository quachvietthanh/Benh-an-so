package com.benhsoan.domain.appointment.exception;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * The doctor has no working schedule for the requested date (NCL-14-CN-003).
 */
public class DoctorScheduleNotFoundException extends AppointmentException {

    public DoctorScheduleNotFoundException(UUID doctorId, LocalDate date) {
        super(DomainErrorCode.DOCTOR_SCHEDULE_UNAVAILABLE,
                "Bác sĩ không có lịch làm việc vào ngày " + date + "."
        );
    }

}
