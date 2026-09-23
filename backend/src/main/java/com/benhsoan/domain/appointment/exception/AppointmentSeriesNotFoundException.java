package com.benhsoan.domain.appointment.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class AppointmentSeriesNotFoundException extends AppointmentException {

    public AppointmentSeriesNotFoundException(UUID seriesId) {
        super(
                DomainErrorCode.APPOINTMENT_SERIES_NOT_FOUND,
                "Không tìm thấy liệu trình đặt lịch với id: " + seriesId
        );
    }
}
