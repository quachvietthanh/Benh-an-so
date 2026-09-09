package com.benhsoan.domain.appointment.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class DoctorTimeOffNotFoundException extends AppointmentException {

    public DoctorTimeOffNotFoundException(UUID timeOffId) {
        super(DomainErrorCode.DOCTOR_TIMEOFF_NOT_FOUND, "Không tìm thấy khoảng nghỉ của bác sĩ: " + timeOffId);
    }
}
