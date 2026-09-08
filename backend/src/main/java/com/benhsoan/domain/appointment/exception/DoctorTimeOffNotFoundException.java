package com.benhsoan.domain.appointment.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class DoctorTimeOffNotFoundException extends AppointmentException {

    public DoctorTimeOffNotFoundException(UUID id) {
        super(DomainErrorCode.DOCTOR_TIME_OFF_NOT_FOUND, "Khoảng nghỉ của bác sĩ không tồn tại: " + id);
    }
}
