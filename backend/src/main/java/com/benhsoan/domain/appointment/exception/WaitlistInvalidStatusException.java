package com.benhsoan.domain.appointment.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class WaitlistInvalidStatusException extends AppointmentException {

    public WaitlistInvalidStatusException() {
        super(DomainErrorCode.WAITLIST_INVALID_STATUS,
                "Trạng thái danh sách chờ không hợp lệ cho thao tác này.");
    }

    public WaitlistInvalidStatusException(String message) {
        super(DomainErrorCode.WAITLIST_INVALID_STATUS, message);
    }
}
