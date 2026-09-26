package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class DoctorHasAvailableSlotsException extends AppointmentException {

    public DoctorHasAvailableSlotsException() {
        super(DomainErrorCode.DOCTOR_HAS_AVAILABLE_SLOTS,
                "Bác sĩ vẫn còn khung giờ trống trong ngày, vui lòng đặt lịch trực tiếp thay vì ghi vào danh sách chờ.");
    }

    public DoctorHasAvailableSlotsException(String message) {
        super(DomainErrorCode.DOCTOR_HAS_AVAILABLE_SLOTS, message);
    }
}
